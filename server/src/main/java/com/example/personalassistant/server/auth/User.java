package com.example.personalassistant.server.auth;

public record User(
        long id,
        String username,
        String phone,
        String passwordHash,
        String displayName,
        int status
) {
}
