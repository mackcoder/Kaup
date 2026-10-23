package com.kaup.auth;

import java.util.UUID;

public record UserResponse(UUID id, String name, String email) {
    public static UserResponse from(UserAccount account) {
        return new UserResponse(account.getId(), account.getName(), account.getEmail());
    }
}
