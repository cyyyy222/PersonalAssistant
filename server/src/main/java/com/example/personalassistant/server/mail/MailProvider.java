package com.example.personalassistant.server.mail;

public record MailProvider(
        String domain,
        String imapHost,
        int imapPort,
        String smtpHost,
        int smtpPort
) {
}
