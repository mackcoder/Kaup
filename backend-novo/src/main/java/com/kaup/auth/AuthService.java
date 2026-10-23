package com.kaup.auth;

import java.time.Clock;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.kaup.security.CurrentUser;

@Service
public class AuthService implements UserDetailsService {
    private final UserAccountRepository accounts;
    private final PasswordEncoder passwords;
    private final CurrentUser currentUser;
    private final Clock clock;

    public AuthService(UserAccountRepository accounts, PasswordEncoder passwords, CurrentUser currentUser, Clock clock) {
        this.accounts = accounts;
        this.passwords = passwords;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Transactional
    public UserResponse register(AuthRequests.Register input) {
        UUID id = UUID.randomUUID();
        String hash = passwords.encode(input.password());
        if (accounts.insertIfAbsent(id, input.email(), input.name(), hash, clock.instant()) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new UserResponse(id, input.name(), input.email());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        UserAccount account = accounts.findByEmail(AuthRequests.normalizeEmail(email))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        return User.withUsername(account.getId().toString()).password(account.getPasswordHash())
                .disabled(!account.isEnabled()).roles("USER").build();
    }

    public UserResponse me(Authentication authentication) {
        return UserResponse.from(currentUser.requireAccount(authentication));
    }
}
