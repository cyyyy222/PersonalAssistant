package com.example.personalassistant.server.mail;

import com.example.personalassistant.server.mail.MailDtos.MessageDetail;
import com.example.personalassistant.server.mail.MailDtos.MessagePage;
import com.example.personalassistant.server.mail.MailDtos.MessageSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class MailMessageRepository {

    public record UpsertResult(long id, boolean created) {
    }

    public record MessageRef(
            long id,
            long accountId,
            String folder,
            long uid,
            String messageId,
            String subject,
            String fromAddress
    ) {
    }

    private static final RowMapper<MessageSummary> SUMMARY_MAPPER = (rs, rowNum) -> new MessageSummary(
            rs.getLong("id"),
            rs.getString("subject"),
            rs.getString("from_name"),
            rs.getString("from_address"),
            timestamp(rs.getTimestamp("sent_at")),
            rs.getString("snippet"),
            rs.getBoolean("is_read"),
            rs.getBoolean("is_starred"),
            rs.getBoolean("has_attachments")
    );

    private static final RowMapper<MessageDetail> DETAIL_MAPPER = (rs, rowNum) -> new MessageDetail(
            rs.getLong("id"),
            rs.getString("subject"),
            rs.getString("from_name"),
            rs.getString("from_address"),
            rs.getString("to_addresses"),
            timestamp(rs.getTimestamp("sent_at")),
            rs.getString("body_text"),
            rs.getBoolean("is_read"),
            rs.getBoolean("is_starred"),
            rs.getBoolean("has_attachments"),
            rs.getInt("attachment_count")
    );

    private final JdbcTemplate jdbcTemplate;

    public MailMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void clearFolder(long accountId, String folder) {
        jdbcTemplate.update("DELETE FROM mail_messages WHERE account_id = ? AND folder = ?", accountId, folder);
    }

    public UpsertResult upsert(long accountId, String folder, ParsedMail mail) {
        Long existingId = jdbcTemplate.query(
                "SELECT id FROM mail_messages WHERE account_id = ? AND folder = ? AND uid = ?",
                (rs, rowNum) -> rs.getLong("id"), accountId, folder, mail.uid()
        ).stream().findFirst().orElse(null);

        if (existingId == null) {
            jdbcTemplate.update(
                    "INSERT INTO mail_messages (account_id, folder, uid, message_id, subject, from_name, from_address, " +
                            "to_addresses, sent_at, received_at, snippet, body_text, body_html, has_attachments, " +
                            "attachment_count, is_read, is_starred, is_archived, raw_size) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?)",
                    accountId, folder, mail.uid(), mail.messageId(), mail.subject(), mail.fromName(),
                    mail.fromAddress(), mail.toAddresses(), time(mail.sentAt()), time(mail.receivedAt()),
                    mail.snippet(), mail.bodyText(), mail.bodyHtml(), mail.hasAttachments(),
                    mail.attachmentCount(), mail.read(), mail.starred(), mail.rawSize()
            );
            Long id = jdbcTemplate.queryForObject(
                    "SELECT id FROM mail_messages WHERE account_id = ? AND folder = ? AND uid = ?",
                    Long.class, accountId, folder, mail.uid()
            );
            return new UpsertResult(id == null ? 0 : id, true);
        }

        jdbcTemplate.update(
                "UPDATE mail_messages SET message_id = ?, subject = ?, from_name = ?, from_address = ?, " +
                        "to_addresses = ?, sent_at = ?, received_at = ?, snippet = ?, body_text = ?, body_html = ?, " +
                        "has_attachments = ?, attachment_count = ?, is_read = ?, is_starred = ?, raw_size = ? " +
                        "WHERE id = ?",
                mail.messageId(), mail.subject(), mail.fromName(), mail.fromAddress(), mail.toAddresses(),
                time(mail.sentAt()), time(mail.receivedAt()), mail.snippet(), mail.bodyText(), mail.bodyHtml(),
                mail.hasAttachments(), mail.attachmentCount(), mail.read(), mail.starred(), mail.rawSize(),
                existingId
        );
        return new UpsertResult(existingId, false);
    }

    public MessagePage list(long accountId, String folder, int page, int size, String keyword,
                            boolean unreadOnly, Boolean starred, Boolean archived) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        StringBuilder where = new StringBuilder(
                " FROM mail_messages m WHERE m.account_id = ? AND m.is_deleted = 0"
        );
        List<Object> params = new ArrayList<>();
        params.add(accountId);

        if (Boolean.TRUE.equals(archived)) {
            where.append(" AND m.is_archived = 1");
        } else {
            where.append(" AND m.folder = ?");
            params.add(folder);
            if (Boolean.FALSE.equals(archived)) {
                where.append(" AND m.is_archived = 0");
            }
        }

        if (unreadOnly) {
            where.append(" AND m.is_read = 0");
        }
        if (starred != null) {
            where.append(" AND m.is_starred = ?");
            params.add(starred);
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (m.subject LIKE ? OR m.from_name LIKE ? OR m.from_address LIKE ? OR m.snippet LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*)" + where, Long.class, params.toArray());
        long count = total == null ? 0 : total;
        List<Object> listParams = new ArrayList<>(params);
        listParams.add(safeSize);
        listParams.add((safePage - 1) * safeSize);

        List<MessageSummary> items = jdbcTemplate.query(
                "SELECT m.id, m.subject, m.from_name, m.from_address, m.sent_at, m.snippet, " +
                        "m.is_read, m.is_starred, m.has_attachments" + where +
                        " ORDER BY COALESCE(m.sent_at, m.received_at, m.created_at) DESC LIMIT ? OFFSET ?",
                SUMMARY_MAPPER, listParams.toArray()
        );
        return new MessagePage(items, safePage, safeSize, count, (long) safePage * safeSize < count);
    }

    public Optional<MessageDetail> findDetail(long messageId, long userId) {
        return jdbcTemplate.query(
                "SELECT m.id, m.subject, m.from_name, m.from_address, m.to_addresses, m.sent_at, m.body_text, " +
                        "m.is_read, m.is_starred, m.has_attachments, m.attachment_count " +
                        "FROM mail_messages m JOIN mail_accounts a ON a.id = m.account_id " +
                        "WHERE m.id = ? AND a.user_id = ? AND m.is_deleted = 0",
                DETAIL_MAPPER, messageId, userId
        ).stream().findFirst();
    }

    public Optional<MessageRef> findRef(long messageId, long userId) {
        return jdbcTemplate.query(
                "SELECT m.id, m.account_id, m.folder, m.uid, m.message_id, m.subject, m.from_address " +
                        "FROM mail_messages m JOIN mail_accounts a ON a.id = m.account_id " +
                        "WHERE m.id = ? AND a.user_id = ? AND m.is_deleted = 0",
                (rs, rowNum) -> new MessageRef(
                        rs.getLong("id"), rs.getLong("account_id"), rs.getString("folder"),
                        rs.getLong("uid"), rs.getString("message_id"), rs.getString("subject"),
                        rs.getString("from_address")
                ), messageId, userId
        ).stream().findFirst();
    }

    public void markArchived(long messageId, long userId, String targetFolder) {
        jdbcTemplate.update(
                "UPDATE mail_messages m JOIN mail_accounts a ON a.id = m.account_id " +
                        "SET m.folder = ?, m.is_archived = 1 WHERE m.id = ? AND a.user_id = ?",
                targetFolder, messageId, userId
        );
    }

    public void markRead(long messageId, long userId, boolean read) {
        jdbcTemplate.update(
                "UPDATE mail_messages m JOIN mail_accounts a ON a.id = m.account_id " +
                        "SET m.is_read = ? WHERE m.id = ? AND a.user_id = ?",
                read, messageId, userId
        );
    }

    public void markStarred(long messageId, long userId, boolean starred) {
        jdbcTemplate.update(
                "UPDATE mail_messages m JOIN mail_accounts a ON a.id = m.account_id " +
                        "SET m.is_starred = ? WHERE m.id = ? AND a.user_id = ?",
                starred, messageId, userId
        );
    }

    public void deleteByAccount(long accountId) {
        jdbcTemplate.update("DELETE FROM mail_messages WHERE account_id = ?", accountId);
    }

    private static Timestamp time(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private static LocalDateTime timestamp(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
