package com.example.personalassistant.server.mail;

import java.time.LocalDateTime;
import java.util.List;

public record ParsedMail(
        long uid,
        String messageId,
        String subject,
        String fromName,
        String fromAddress,
        String toAddresses,
        LocalDateTime sentAt,
        LocalDateTime receivedAt,
        String snippet,
        String bodyText,
        String bodyHtml,
        boolean hasAttachments,
        int attachmentCount,
        boolean read,
        boolean starred,
        int rawSize,
        List<ParsedAttachment> attachments
) {
}
