package com.example.personalassistant.server.mail.storage;

public record StoredObject(
        String provider,
        String key,
        long size,
        String checksum
) {
}
