package com.kaup.security;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class OwnerAuthorization {
    private final CurrentUser currentUser;

    public OwnerAuthorization(CurrentUser currentUser) { this.currentUser = currentUser; }

    // ownerId must come from the stored resource, never from a claimed owner in the request.
    public UUID requireOwner(Authentication authentication, UUID ownerId) {
        UUID userId = currentUser.requireId(authentication);
        if (!userId.equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return userId;
    }
}
