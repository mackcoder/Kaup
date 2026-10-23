package com.kaup.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class AuthRequestsTest {
    @Test
    void rejectsWeakOversizedAndInvalidRegistrationData() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new AuthRequests.Register("Ana", "ana@example.test", "valid-password-123"))).isEmpty();
            for (String password : new String[] {"short", " ".repeat(12), "a".repeat(73), "é".repeat(37), "🔒".repeat(6)}) {
                assertThat(validator.validate(new AuthRequests.Register("Ana", "ana@example.test", password))).isNotEmpty();
            }
            assertThat(validator.validate(new AuthRequests.Register(" ", "invalid-email", "valid-password-123"))).isNotEmpty();
            assertThat(validator.validate(new AuthRequests.Login("ana@example.test", "é".repeat(37)))).isNotEmpty();
            assertThat(validator.validate(new AuthRequests.Login("ana@example.test", "é".repeat(36)))).isEmpty();
        }
    }

    @Test
    void neverTrimsPasswordOrExposesItThroughSerializationOrToString() throws Exception {
        String password = "  synthetic-password  ";
        var input = new AuthRequests.Register("Ana", "ana@example.test", password);
        assertThat(input.password()).isEqualTo(password);
        assertThat(input.toString()).doesNotContain(password, "ana@example.test");
        assertThat(new ObjectMapper().writeValueAsString(input)).doesNotContain(password, "\"password\"");
        assertThat(new AuthRequests.Login("ana@example.test", password).toString()).doesNotContain(password);
    }
}
