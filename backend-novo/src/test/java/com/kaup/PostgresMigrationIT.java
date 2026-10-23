package com.kaup;

import java.util.UUID;
import java.net.URI;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PostgresMigrationIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.9-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;
    @Autowired TestRestTemplate http;

    @Test
    void migratesFreshPostgresAndDoesNotReapplyMigration() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");
        assertThat(flyway.info().pending()).isEmpty();
        flyway.validate();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void storesUuidAndTimestampAndRejectsDuplicateEmail() {
        String email = "fixture-" + UUID.randomUUID() + "@example.test";
        UUID id = UUID.randomUUID();
        insert(id, email);
        try {
            assertThat(jdbc.queryForObject("SELECT enabled FROM users WHERE id = ?", Boolean.class, id)).isTrue();
            assertThat(jdbc.queryForObject("SELECT created_at IS NOT NULL FROM users WHERE id = ?", Boolean.class, id))
                    .isTrue();
            assertThatThrownBy(() -> insert(UUID.randomUUID(), email)).isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            jdbc.update("DELETE FROM users WHERE id = ?", id);
        }
    }

    @Test
    void rejectsUnnormalizedEmailAndBlankHash() {
        assertThatThrownBy(() -> insert(UUID.randomUUID(), " Uppercase@example.test "))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO users (id, email, password_hash, name) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), "blank@example.test", " ", "Teste"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void servesRealHttpAfterDatabaseStartupAndProtectsPrivateResources() {
        var health = http.getForEntity("/api/analise/health", String.class);
        assertThat(health.getStatusCode().value()).isEqualTo(200);
        assertThat(health.getBody()).isEqualTo("Backend do Kaup está rodando.");
        var denied = http.getForEntity("/api/perfil", String.class);
        assertThat(denied.getStatusCode().value()).isEqualTo(401);
        assertThat(denied.getBody()).contains("UNAUTHORIZED", "requestId");
    }

    @ParameterizedTest
    @ValueSource(strings = {"TRACE", "INVALID"})
    void rejectedHttpMethodsReturnJsonWithMatchingRequestId(String method) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(http.getRootUri() + "/api/analise/health"))
                .method(method, HttpRequest.BodyPublishers.noBody())
                .header("Accept", "text/html")
                .build();
        var response = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(value ->
                assertThat(value).startsWith("application/json"));
        String requestId = response.headers().firstValue("X-Request-Id").orElseThrow();
        assertThat(UUID.fromString(requestId)).isNotNull();
        assertThat(response.body()).contains("\"code\":\"VALIDATION_ERROR\"",
                "\"requestId\":\"" + requestId + "\"");
    }

    @Test
    void malformedHttpDoesNotEchoInputInResponseOrLogs(CapturedOutput output) throws Exception {
        String marker = "synthetic-private-query-marker";
        URI server = URI.create(http.getRootUri());
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress(server.getHost(), server.getPort()), 5000);
            socket.setSoTimeout(5000);
            // An invalid request target is rejected by Tomcat before servlet filters run.
            String request = "GET /api/analise/health?data=<" + marker + "> HTTP/1.1\r\n"
                    + "Host: localhost\r\nConnection: close\r\n\r\n";
            socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
            String response = new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(response).startsWith("HTTP/1.1 400")
                    .doesNotContain(marker, "java.lang.", "Apache Tomcat/");
        }
        assertThat(output.getAll()).doesNotContain(marker);
    }

    private void insert(UUID id, String email) {
        // Synthetic marker, not a usable password hash or a seeded account.
        jdbc.update("INSERT INTO users (id, email, password_hash, name) VALUES (?, ?, ?, ?)", id, email, "test-only-marker", "Teste");
    }
}
