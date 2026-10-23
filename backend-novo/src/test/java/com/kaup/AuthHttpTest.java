package com.kaup;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.kaup.auth.*;
import com.kaup.common.error.ApiExceptionHandler;
import com.kaup.common.web.RequestIdFilter;
import com.kaup.security.SecurityConfig;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class, RequestIdFilter.class})
class AuthHttpTest {
    @Autowired MockMvc mvc;
    @MockitoBean AuthService accounts;
    @MockitoBean AuthSessionService sessions;
    @MockitoBean AuthRateLimiter limiter;

    private static final String REGISTER = "{\"name\":\"Ana\",\"email\":\"ana@example.test\",\"password\":\"synthetic-password\"}";
    private static final String LOGIN = "{\"email\":\"ana@example.test\",\"password\":\"synthetic-password\"}";

    @Test
    void issuesCsrfWithoutAuthenticationAndDoesNotCacheIt() throws Exception {
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk())
                .andExpect(jsonPath("headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("token").isNotEmpty())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    }

    @Test
    void registrationRequiresCsrfAndReturnsOnlySafeUserFields() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER))
                .andExpect(status().isForbidden());
        verifyNoInteractions(accounts);
        when(accounts.register(any())).thenReturn(new UserResponse(UUID.randomUUID(), "Ana", "ana@example.test"));
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(REGISTER))
                .andExpect(status().isCreated()).andExpect(jsonPath("id").isNotEmpty())
                .andExpect(jsonPath("password").doesNotExist()).andExpect(jsonPath("passwordHash").doesNotExist());
    }

    @Test
    void invalidPayloadAndContentTypeAreControlledErrors() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("VALIDATION_ERROR"));
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_FORM_URLENCODED).content("password=synthetic"))
                .andExpect(status().isUnsupportedMediaType());
        verifyNoInteractions(sessions, accounts);
    }

    @Test
    void loginFailureIsGeneric401AndRateLimitIs429() throws Exception {
        when(sessions.login(any(), any(), any())).thenThrow(new BadCredentialsException("private-auth-detail"));
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("code").value("UNAUTHORIZED"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-auth-detail"))));
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS)).when(limiter).check(any());
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(REGISTER))
                .andExpect(status().isTooManyRequests()).andExpect(jsonPath("code").value("RATE_LIMITED"));
        verifyNoInteractions(accounts);
    }

    @Test
    void meRequiresSessionAndLogoutCannotBeWrittenWithoutCsrf() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout")).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login").with(csrf().useInvalidToken()).contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isForbidden());
        verifyNoInteractions(sessions, accounts);
    }
}
