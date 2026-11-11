package com.example.personalassistant.server.mail;

public record ParsedAttachment(
        int partIndex,
        String filename,
        String contentType,
        long size,
        String contentId
) {
}
