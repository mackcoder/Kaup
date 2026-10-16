package com.kaup.auth;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthRequests {
    private AuthRequests() { }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    public record Register(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 12, max = 72)
            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password) {
        public Register {
            name = name == null ? null : name.strip();
            email = normalizeEmail(email);
        }

        @AssertTrue
        public boolean isPasswordWithinLimits() {
            return withinByteLimit(password) && (password == null || password.codePointCount(0, password.length()) >= 12);
        }

        @Override
        public String toString() { return "Register[redacted]"; }
    }

    public record Login(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 72)
            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password) {
        public Login { email = normalizeEmail(email); }

        @AssertTrue
        public boolean isPasswordWithinByteLimit() { return withinByteLimit(password); }

        @Override
        public String toString() { return "Login[redacted]"; }
    }

    private static boolean withinByteLimit(String password) {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
