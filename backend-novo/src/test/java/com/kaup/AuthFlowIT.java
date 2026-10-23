package com.kaup;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.Session;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.kaup.security.OwnerAuthorization;

import static org.assertj.core.api.Assertions.*;

@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "server.servlet.session.cookie.secure=false", "auth.bcrypt-strength=10", "auth.max-attempts=1000"})
@Import(AuthFlowIT.OwnedFixtureController.class)
class AuthFlowIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.9-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired TestRestTemplate address;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired JdbcIndexedSessionRepository sessions;
    private static final String PASSWORD = "synthetic-password-123";

    @BeforeEach
    void fixtureSchema() {
        // This table and its routes exist only in tests, not in production migrations.
        jdbc.execute("CREATE TABLE IF NOT EXISTS test_owned_resources (id UUID PRIMARY KEY, owner_id UUID NOT NULL REFERENCES users(id))");
    }

    @Test
    void registerLoginRotateCsrfAndLogoutPersistedSession(CapturedOutput output) throws Exception {
        var client = new Client();
        assertThat(client.send("GET", "/api/auth/me", null, null).statusCode()).isEqualTo(401);
        var csrfResponse = client.csrf();
        assertThat(csrfResponse.headers().allValues("Set-Cookie").get(0))
                .contains("KAUPSESSION=", "Path=/", "HttpOnly", "SameSite=Lax").doesNotContain("Domain=", "Secure");
        String beforeLogin = client.cookie(), oldToken = client.token;
        client.register();
        assertThat(client.send("GET", "/api/auth/me", null, null).statusCode()).isEqualTo(401);
        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE id = ?", String.class, client.id);
        assertThat(hash).startsWith("$2a$10$").isNotEqualTo(PASSWORD);
        assertThat(passwords.matches(PASSWORD, hash)).isTrue();
        var login = client.login(PASSWORD);
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(json(login).path("id").asText()).isEqualTo(client.id.toString());
        assertThat(login.body()).doesNotContain(PASSWORD, hash, "passwordHash", "password_hash");
        assertThat(client.cookie()).isNotEqualTo(beforeLogin);
        assertThat(replay(beforeLogin).statusCode()).isEqualTo(401);
        assertThat(client.send("GET", "/api/auth/me", null, null).statusCode()).isEqualTo(200);
        assertThat(client.send("POST", "/api/auth/logout", null, oldToken).statusCode()).isEqualTo(403);

        String authenticatedCookie = client.cookie();
        Session stored = sessions.findById(authenticatedCookie);
        assertThat(stored).isNotNull();
        SecurityContext context = stored.getAttribute("SPRING_SECURITY_CONTEXT");
        assertThat(context.getAuthentication().getName()).isEqualTo(client.id.toString());
        assertThat(context.getAuthentication().getCredentials()).isNull();
        assertThat(((UserDetails) context.getAuthentication().getPrincipal()).getPassword()).isNull();
        assertThat(jdbc.queryForObject("SELECT principal_name FROM spring_session WHERE session_id = ?", String.class, authenticatedCookie))
                .isEqualTo(client.id.toString());
        for (byte[] attribute : jdbc.query("SELECT attribute_bytes FROM spring_session_attributes WHERE session_primary_id = "
                + "(SELECT primary_id FROM spring_session WHERE session_id = ?)", (rs, row) -> rs.getBytes(1), authenticatedCookie)) {
            assertThat(new String(attribute, StandardCharsets.ISO_8859_1)).doesNotContain(PASSWORD, hash, client.email);
        }

        client.csrf();
        var logout = client.send("POST", "/api/auth/logout", null, client.token);
        assertThat(logout.statusCode()).isEqualTo(204);
        assertThat(logout.body()).isEmpty();
        assertThat(logout.headers().allValues("Set-Cookie")).anySatisfy(cookie ->
                assertThat(cookie).contains("KAUPSESSION=", "Max-Age=0", "HttpOnly", "SameSite=Lax"));
        assertThat(sessions.findById(authenticatedCookie)).isNull();
        assertThat(replay(authenticatedCookie).statusCode()).isEqualTo(401);
        assertThat(client.send("GET", "/api/auth/me", null, null).statusCode()).isEqualTo(401);
        String loggedOutToken = client.token;
        client.csrf();
        assertThat(client.send("POST", "/api/auth/login", Map.of("email", client.email, "password", PASSWORD), loggedOutToken).statusCode()).isEqualTo(403);
        assertThat(output.getAll()).doesNotContain(PASSWORD, hash, client.email);
    }

    @Test
    void writesRequireCsrfIncludingRegisterLoginAndLogout() throws Exception {
        var client = new Client();
        for (String route : List.of("register", "login", "logout")) {
            assertThat(client.send("POST", "/api/auth/" + route,
                    Map.of("name", "A", "email", client.email, "password", PASSWORD), null).statusCode()).isEqualTo(403);
        }
        client.csrf();
        var other = new Client();
        other.csrf();
        assertThat(other.send("POST", "/api/auth/register",
                Map.of("name", "A", "email", client.email, "password", PASSWORD), client.token).statusCode()).isEqualTo(403);
    }

    @Test
    void wrongMissingAndDisabledAccountsUseTheSameGeneric401() throws Exception {
        var client = new Client();
        client.csrf();
        client.register();
        var wrong = client.login("incorrect-password");
        assertThat(wrong.statusCode()).isEqualTo(401);
        var missing = client.send("POST", "/api/auth/login", Map.of("email", "missing@example.test", "password", PASSWORD), client.token);
        jdbc.update("UPDATE users SET enabled = false WHERE id = ?", client.id);
        var disabled = client.login(PASSWORD);
        for (var response : List.of(missing, disabled)) {
            assertThat(response.statusCode()).isEqualTo(401);
            assertThat(json(response).path("code")).isEqualTo(json(wrong).path("code"));
            assertThat(json(response).path("message")).isEqualTo(json(wrong).path("message"));
        }
        assertThat(client.send("GET", "/api/auth/me", null, null).statusCode()).isEqualTo(401);
    }

    @Test
    void expiredOrDisabledSessionsCannotIdentifyUser() throws Exception {
        var expired = loggedIn();
        jdbc.update("UPDATE spring_session SET last_access_time = 0, expiry_time = 0 WHERE session_id = ?", expired.cookie());
        assertThat(expired.send("GET", "/api/auth/me", null, null).statusCode()).isEqualTo(401);
        var disabled = loggedIn();
        jdbc.update("UPDATE users SET enabled = false WHERE id = ?", disabled.id);
        assertThat(disabled.send("GET", "/api/auth/me", null, null).statusCode()).isEqualTo(401);
    }

    @Test
    void separateAccountsCannotReadOrDeleteEachOthersOwnedFixture() throws Exception {
        var a = loggedIn();
        var b = loggedIn();
        UUID resource = UUID.randomUUID();
        jdbc.update("INSERT INTO test_owned_resources (id, owner_id) VALUES (?, ?)", resource, a.id);
        String route = "/test/owned/" + resource;
        assertThat(a.send("GET", route, null, null).statusCode()).isEqualTo(200);
        var denied = b.send("GET", route + "?ownerId=" + b.id, null, null);
        var absent = b.send("GET", "/test/owned/" + UUID.randomUUID(), null, null);
        assertThat(denied.statusCode()).isEqualTo(404);
        assertThat(absent.statusCode()).isEqualTo(404);
        assertThat(json(denied).path("message")).isEqualTo(json(absent).path("message"));
        assertThat(b.send("DELETE", route, null, b.token).statusCode()).isEqualTo(404);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM test_owned_resources WHERE id = ?", Integer.class, resource)).isEqualTo(1);
        assertThat(a.send("DELETE", route, null, a.token).statusCode()).isEqualTo(204);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM test_owned_resources WHERE id = ?", Integer.class, resource)).isZero();
        assertThat(json(a.send("GET", "/api/auth/me", null, null)).path("id").asText()).isEqualTo(a.id.toString());
        assertThat(json(b.send("GET", "/api/auth/me", null, null)).path("id").asText()).isEqualTo(b.id.toString());
    }

    @Test
    void concurrentDuplicateRegistrationCreatesOneNormalizedAccount(CapturedOutput output) throws Exception {
        var a = new Client();
        var b = new Client();
        a.csrf(); b.csrf();
        String email = "concurrent-" + UUID.randomUUID() + "@example.test";
        var executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Integer>> tasks = List.of(
                    () -> a.send("POST", "/api/auth/register", Map.of("name", "A", "email", email, "password", PASSWORD), a.token).statusCode(),
                    () -> b.send("POST", "/api/auth/register", Map.of("name", "B", "email", " " + email.toUpperCase() + " ", "password", PASSWORD), b.token).statusCode());
            var results = executor.invokeAll(tasks);
            assertThat(List.of(results.get(0).get(), results.get(1).get())).containsExactlyInAnyOrder(201, 409);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE email = ?", Integer.class, email)).isEqualTo(1);
            assertThat(output.getAll()).doesNotContain(email, PASSWORD);
        } finally { executor.shutdownNow(); }
    }

    @Test
    void getLogoutDoesNotMutateSessionAndForgedCookiesCannotAuthenticate() throws Exception {
        var client = loggedIn();
        assertThat(client.send("GET", "/api/auth/logout", null, null).statusCode()).isEqualTo(404);
        assertThat(client.send("GET", "/api/auth/me", null, null).statusCode()).isEqualTo(200);
        assertThat(replay(UUID.randomUUID().toString()).statusCode()).isEqualTo(401);
    }

    @Test
    void rejectsPasswordsAboveUtf8ByteLimitWithoutEcho() throws Exception {
        var client = new Client();
        client.csrf();
        String oversized = "é".repeat(37);
        var response = client.send("POST", "/api/auth/register", Map.of("name", "A", "email", client.email, "password", oversized), client.token);
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).doesNotContain(oversized, client.email);
    }

    @Test
    void reauthenticationChangesIdentityAndRevokesThePreviousSessionId() throws Exception {
        var a = loggedIn();
        var b = loggedIn();
        String oldCookie = a.cookie();
        var response = a.send("POST", "/api/auth/login", Map.of("email", b.email, "password", PASSWORD), a.token);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(a.cookie()).isNotEqualTo(oldCookie);
        assertThat(replay(oldCookie).statusCode()).isEqualTo(401);
        assertThat(json(a.send("GET", "/api/auth/me", null, null)).path("id").asText()).isEqualTo(b.id.toString());
    }

    private Client loggedIn() throws Exception {
        var client = new Client();
        client.csrf(); client.register();
        assertThat(client.login(PASSWORD).statusCode()).isEqualTo(200);
        client.csrf();
        return client;
    }

    private JsonNode json(HttpResponse<String> response) throws Exception { return mapper.readTree(response.body()); }

    private HttpResponse<String> replay(String cookie) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(address.getRootUri() + "/api/auth/me"))
                .timeout(Duration.ofSeconds(10)).header("Cookie", "KAUPSESSION=" + cookie).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private class Client {
        final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        final HttpClient http = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(5)).build();
        final String email = "fixture-" + UUID.randomUUID() + "@example.test";
        UUID id;
        String token;

        HttpResponse<String> send(String method, String route, Object payload, String csrf) throws Exception {
            var request = HttpRequest.newBuilder(URI.create(address.getRootUri() + route)).timeout(Duration.ofSeconds(10));
            if (csrf != null) { request.header("X-CSRF-TOKEN", csrf); }
            if (payload != null) { request.header("Content-Type", "application/json"); }
            request.method(method, payload == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)));
            return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> csrf() throws Exception {
            var response = send("GET", "/api/auth/csrf", null, null);
            assertThat(response.statusCode()).isEqualTo(200);
            token = json(response).path("token").asText();
            return response;
        }

        void register() throws Exception {
            var response = send("POST", "/api/auth/register", Map.of("name", "Pessoa sintética", "email", email, "password", PASSWORD), token);
            assertThat(response.statusCode()).isEqualTo(201);
            id = UUID.fromString(json(response).path("id").asText());
        }

        HttpResponse<String> login(String password) throws Exception {
            return send("POST", "/api/auth/login", Map.of("email", email, "password", password), token);
        }

        String cookie() { return cookies.getCookieStore().getCookies().stream().filter(cookie -> cookie.getName().equals("KAUPSESSION"))
                .findFirst().orElseThrow().getValue(); }
    }

    @RestController
    static class OwnedFixtureController {
        private final JdbcTemplate jdbc;
        private final OwnerAuthorization owners;

        OwnedFixtureController(JdbcTemplate jdbc, OwnerAuthorization owners) { this.jdbc = jdbc; this.owners = owners; }

        @GetMapping("/test/owned/{id}")
        Map<String, UUID> read(@PathVariable UUID id, Authentication authentication) {
            return Map.of("ownerId", authorize(id, authentication));
        }

        @DeleteMapping("/test/owned/{id}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void delete(@PathVariable UUID id, Authentication authentication) {
            UUID owner = authorize(id, authentication);
            jdbc.update("DELETE FROM test_owned_resources WHERE id = ? AND owner_id = ?", id, owner);
        }

        private UUID authorize(UUID id, Authentication authentication) {
            UUID owner = jdbc.query("SELECT owner_id FROM test_owned_resources WHERE id = ?", (rs, row) -> rs.getObject(1, UUID.class), id)
                    .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            return owners.requireOwner(authentication, owner);
        }
    }
}
