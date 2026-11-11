package com.example.personalassistant.server.mail;

public record MailAttachment(
        long id,
        long messageId,
        int partIndex,
        String filename,
        String contentType,
        long size,
        String storageProvider,
        String storageKey,
        String contentId,
        String checksum
) {
}
