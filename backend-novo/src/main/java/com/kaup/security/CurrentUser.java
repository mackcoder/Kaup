package com.kaup.security;

import java.util.UUID;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kaup.auth.UserAccount;
import com.kaup.auth.UserAccountRepository;

@Component
public class CurrentUser {
    private final UserAccountRepository accounts;

    public CurrentUser(UserAccountRepository accounts) { this.accounts = accounts; }

    @Transactional(readOnly = true)
    public UserAccount requireAccount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw unauthorized();
        }
        UUID id;
        try {
            id = UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException exception) {
            throw unauthorized();
        }
        return accounts.findById(id).filter(UserAccount::isEnabled).orElseThrow(CurrentUser::unauthorized);
    }

    public UUID requireId(Authentication authentication) { return requireAccount(authentication).getId(); }

    private static AuthenticationCredentialsNotFoundException unauthorized() {
        return new AuthenticationCredentialsNotFoundException("Authentication required");
    }
}
