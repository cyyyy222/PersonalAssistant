package com.example.personalassistant.server.mail;

import java.time.LocalDateTime;

public record MailAccount(
        long id,
        long userId,
        String email,
        String displayName,
        String imapHost,
        int imapPort,
        boolean imapSsl,
        String imapUsername,
        String smtpHost,
        int smtpPort,
        boolean smtpSsl,
        String secretCipher,
        int secretKeyVersion,
        int status,
        String archiveFolder,
        String sentFolder,
        LocalDateTime lastSyncAt,
        String lastError
) {
}
