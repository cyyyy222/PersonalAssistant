package com.example.personalassistant.server.mail;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public final class MailDtos {

    private MailDtos() {
    }

    public record BindRequest(
            @NotBlank(message = "请输入邮箱地址") String email,
            @NotBlank(message = "请输入邮箱授权码") String secret,
            String displayName
    ) {
    }

    public record AccountView(
            long id,
            String email,
            String displayName,
            int status,
            LocalDateTime lastSyncAt
    ) {
        public static AccountView from(MailAccount account) {
            return new AccountView(account.id(), account.email(), account.displayName(),
                    account.status(), account.lastSyncAt());
        }
    }

    public record SyncResult(
            int newMessages,
            int updatedMessages,
            boolean uidValidityChanged,
            long lastUid
    ) {
    }

    public record MessageSummary(
            long id,
            String subject,
            String fromName,
            String fromAddress,
            LocalDateTime sentAt,
            String snippet,
            boolean read,
            boolean starred,
            boolean hasAttachments
    ) {
    }

    public record MessagePage(
            List<MessageSummary> items,
            int page,
            int size,
            long total,
            boolean hasMore
    ) {
    }

    public record MessageDetail(
            long id,
            String subject,
            String fromName,
            String fromAddress,
            String toAddresses,
            LocalDateTime sentAt,
            String bodyText,
            boolean read,
            boolean starred,
            boolean hasAttachments,
            int attachmentCount
    ) {
    }

    public record MarkReadRequest(boolean read) {
    }

    public record MarkStarredRequest(boolean starred) {
    }

    public record ArchiveResult(String folder) {
    }

    public record AttachmentView(
            long id,
            String filename,
            String contentType,
            long size
    ) {
        public static AttachmentView from(MailAttachment attachment) {
            return new AttachmentView(attachment.id(), attachment.filename(),
                    attachment.contentType(), attachment.size());
        }
    }

    public record SendRequest(
            @NotNull(message = "请选择发件邮箱") Long accountId,
            @NotBlank(message = "请输入收件人") String to,
            String cc,
            String bcc,
            @NotBlank(message = "请输入主题") String subject,
            @NotBlank(message = "请输入正文") String body,
            Boolean html,
            Long replyToMessageId,
            List<Long> attachmentIds
    ) {
    }

    public record SendResult(String messageId, boolean savedToSent) {
    }
}
