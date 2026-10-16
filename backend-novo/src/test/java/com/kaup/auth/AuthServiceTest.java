package com.kaup.auth;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.kaup.security.CurrentUser;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private final UserAccountRepository accounts = mock(UserAccountRepository.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-16T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void registrationHashesPasswordAndUsesAtomicInsert() {
        PasswordEncoder passwords = mock(PasswordEncoder.class);
        when(passwords.encode("synthetic-password")).thenReturn("encoded-hash");
        when(accounts.insertIfAbsent(any(), any(), any(), any(), any())).thenReturn(1);
        var service = new AuthService(accounts, passwords, currentUser, clock);
        var user = service.register(new AuthRequests.Register("  Ana  ", " ANA@EXAMPLE.TEST ", "synthetic-password"));
        assertThat(user.email()).isEqualTo("ana@example.test");
        assertThat(user.name()).isEqualTo("Ana");
        verify(accounts).insertIfAbsent(eq(user.id()), eq(user.email()), eq(user.name()), eq("encoded-hash"), eq(clock.instant()));
    }

    @Test
    void duplicateRegistrationReportsConflict() {
        PasswordEncoder passwords = mock(PasswordEncoder.class);
        when(passwords.encode(any())).thenReturn("encoded-hash");
        var service = new AuthService(accounts, passwords, currentUser, clock);
        assertThatThrownBy(() -> service.register(new AuthRequests.Register("Ana", "ana@example.test", "synthetic-password")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode().value()).isEqualTo(409));
    }

    @Test
    void authenticatesWithUuidAndErasesHashFromThePrincipal() {
        var configuration = new AuthConfiguration();
        PasswordEncoder passwords = configuration.passwordEncoder(10);
        String hash = passwords.encode("synthetic-password");
        UUID id = UUID.randomUUID();
        when(accounts.findByEmail("ana@example.test"))
                .thenReturn(Optional.of(new UserAccount(id, "ana@example.test", "Ana", hash, true, clock.instant())));
        var service = new AuthService(accounts, passwords, currentUser, clock);
        var manager = configuration.authenticationManager(service, passwords);
        var result = manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("ANA@example.test", "synthetic-password"));
        assertThat(result.getName()).isEqualTo(id.toString());
        assertThat(result.getCredentials()).isNull();
        assertThat(((UserDetails) result.getPrincipal()).getPassword()).isNull();
        assertThatThrownBy(() -> manager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("ana@example.test", "incorrect-password")))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> manager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("missing@example.test", "incorrect-password")))
                .isInstanceOf(BadCredentialsException.class);
        assertThat(hash).isNotEqualTo(passwords.encode("synthetic-password"));
    }
}
