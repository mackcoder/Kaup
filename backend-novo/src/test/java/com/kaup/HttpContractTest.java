package com.kaup;

import java.util.UUID;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.kaup.analysis.HealthController;
import com.kaup.common.error.ApiErrorController;
import com.kaup.common.error.ApiExceptionHandler;
import com.kaup.common.web.RequestIdFilter;
import com.kaup.security.SecurityConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {HealthController.class, ApiErrorController.class, HttpContractTest.ErrorFixture.class},
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import({SecurityConfig.class, RequestIdFilter.class, ApiExceptionHandler.class, HttpContractTest.ErrorFixture.class})
class HttpContractTest {
    @Autowired
    MockMvc mvc;

    @Test
    void publicHealthIsPlainTextWithoutIntegrationClaims() throws Exception {
        var result = mvc.perform(get("/api/analise/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Backend do Kaup está rodando."))
                .andReturn();
        assertThat(UUID.fromString(result.getResponse().getHeader(RequestIdFilter.HEADER))).isNotNull();
    }

    @Test
    void generatesUniqueIdsInsteadOfTrustingClientHeaders() throws Exception {
        var first = mvc.perform(get("/api/analise/health").header(RequestIdFilter.HEADER, "sensitive-client-data"))
                .andReturn().getResponse().getHeader(RequestIdFilter.HEADER);
        var second = mvc.perform(get("/api/analise/health")).andReturn()
                .getResponse().getHeader(RequestIdFilter.HEADER);
        assertThat(first).isNotEqualTo("sensitive-client-data").isNotEqualTo(second);
        assertThat(UUID.fromString(first)).isNotNull();
    }

    @Test
    void privatePathsRequireAuthenticationAndDoNotRedirectToLogin() throws Exception {
        var result = mvc.perform(get("/api/documentos"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("code").value("UNAUTHORIZED"))
                .andReturn();
        assertCorrelation(result);
    }

    @Test
    void csrfErrorsUseTheSameJsonContract() throws Exception {
        var result = mvc.perform(post("/api/analise/health").with(user("synthetic")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("code").value("FORBIDDEN"))
                .andReturn();
        assertCorrelation(result);
    }

    @Test
    void authenticatedUnknownPathReturnsSanitized404() throws Exception {
        var result = mvc.perform(get("/api/missing").with(user("synthetic")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("code").value("NOT_FOUND"))
                .andExpect(jsonPath("path").doesNotExist())
                .andExpect(jsonPath("trace").doesNotExist())
                .andReturn();
        assertCorrelation(result);
    }

    @Test
    void methodErrorsPreserveAllowHeader() throws Exception {
        mvc.perform(post("/api/analise/health").with(user("synthetic")).with(csrf()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void beanValidationDoesNotEchoRejectedValues() throws Exception {
        mvc.perform(post("/test/input").with(user("synthetic")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("errors").doesNotExist());
    }

    @Test
    void malformedJsonIsSanitized() throws Exception {
        mvc.perform(post("/test/input").with(user("synthetic")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("sensitive-json-fragment"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("code").value("VALIDATION_ERROR"))
                .andExpect(content().string(not(containsString("sensitive-json-fragment"))));
    }

    @Test
    void unexpectedFailureDoesNotExposeExceptionOrPayload() throws Exception {
        var result = mvc.perform(get("/test/failure").accept(MediaType.TEXT_HTML).with(user("synthetic")))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("code").value("INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("private-provider-detail"))))
                .andExpect(jsonPath("exception").doesNotExist())
                .andReturn();
        assertCorrelation(result);
    }

    @Test
    void servletErrorDispatchUsesJsonEvenForBrowserAcceptHeader() throws Exception {
        var result = mvc.perform(get("/error").accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/synthetic-missing-resource")
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE, "private-provider-detail")
                        .with(request -> { request.setDispatcherType(DispatcherType.ERROR); return request; }))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("code").value("NOT_FOUND"))
                .andExpect(content().string(not(containsString("private-provider-detail"))))
                .andReturn();
        assertCorrelation(result);
    }

    private void assertCorrelation(MvcResult result) throws Exception {
        String id = result.getResponse().getHeader(RequestIdFilter.HEADER);
        assertThat(UUID.fromString(id)).isNotNull();
        assertThat(result.getResponse().getContentAsString()).contains("\"requestId\":\"" + id + "\"");
    }

    // Deliberately test-only routes to exercise shared error handling; not shipped in the jar.
    @RestController
    static class ErrorFixture {
        record Input(@NotBlank String name) { }

        @PostMapping("/test/input")
        Input input(@Valid @RequestBody Input input) { return input; }

        @GetMapping("/test/failure")
        void failure() { throw new IllegalStateException("private-provider-detail"); }
    }
}
