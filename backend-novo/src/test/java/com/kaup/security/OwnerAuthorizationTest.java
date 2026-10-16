package com.kaup.security;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import com.kaup.auth.UserAccount;
import com.kaup.auth.UserAccountRepository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OwnerAuthorizationTest {
    private final UserAccountRepository accounts = mock(UserAccountRepository.class);
    private final CurrentUser current = new CurrentUser(accounts);

    @Test
    void requiresARealEnabledAccountEvenWithAnAuthenticatedToken() {
        assertThatThrownBy(() -> current.requireId(null)).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        var malformed = UsernamePasswordAuthenticationToken.authenticated("claimed-client-owner", null, List.of());
        assertThatThrownBy(() -> current.requireId(malformed)).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        UUID id = UUID.randomUUID();
        var token = UsernamePasswordAuthenticationToken.authenticated(id.toString(), null, List.of());
        when(accounts.findById(id)).thenReturn(Optional.of(new UserAccount(id, "a@example.test", "A", "hash", false, Instant.now())));
        assertThatThrownBy(() -> current.requireId(token)).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void comparesStoredOwnerWithSessionIdentityAndHidesOtherOwners() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        var token = UsernamePasswordAuthenticationToken.authenticated(a.toString(), null, List.of());
        when(accounts.findById(a)).thenReturn(Optional.of(new UserAccount(a, "a@example.test", "A", "hash", true, Instant.now())));
        var authorization = new OwnerAuthorization(current);
        assertThat(authorization.requireOwner(token, a)).isEqualTo(a);
        assertThatThrownBy(() -> authorization.requireOwner(token, b))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode().value()).isEqualTo(404));
        when(accounts.findById(a)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authorization.requireOwner(token, a)).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }
}
