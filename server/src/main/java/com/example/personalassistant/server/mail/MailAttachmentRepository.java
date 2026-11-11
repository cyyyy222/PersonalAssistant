package com.example.personalassistant.server.mail;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MailAttachmentRepository {

    public record AttachmentSource(
            long id,
            long messageId,
            long accountId,
            String folder,
            long uid,
            int partIndex,
            String filename,
            String contentType,
            long size,
            String storageProvider,
            String storageKey
    ) {
    }

    private static final RowMapper<MailAttachment> MAPPER = (rs, rowNum) -> new MailAttachment(
            rs.getLong("id"),
            rs.getLong("message_id"),
            rs.getInt("part_index"),
            rs.getString("filename"),
            rs.getString("content_type"),
            rs.getLong("size"),
            rs.getString("storage_provider"),
            rs.getString("storage_key"),
            rs.getString("content_id"),
            rs.getString("checksum")
    );

    private static final RowMapper<AttachmentSource> SOURCE_MAPPER = (rs, rowNum) -> new AttachmentSource(
            rs.getLong("id"),
            rs.getLong("message_id"),
            rs.getLong("account_id"),
            rs.getString("folder"),
            rs.getLong("uid"),
            rs.getInt("part_index"),
            rs.getString("filename"),
            rs.getString("content_type"),
            rs.getLong("size"),
            rs.getString("storage_provider"),
            rs.getString("storage_key")
    );

    private final JdbcTemplate jdbcTemplate;

    public MailAttachmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void replaceForMessage(long messageId, List<ParsedAttachment> attachments, String provider) {
        for (ParsedAttachment attachment : attachments) {
            Long existingId = jdbcTemplate.query(
                    "SELECT id FROM mail_attachments WHERE message_id = ? AND part_index = ?",
                    (rs, rowNum) -> rs.getLong("id"), messageId, attachment.partIndex()
            ).stream().findFirst().orElse(null);

            if (existingId == null) {
                jdbcTemplate.update(
                        "INSERT INTO mail_attachments (message_id, part_index, filename, content_type, size, " +
                                "storage_provider, storage_key, content_id) VALUES (?, ?, ?, ?, ?, ?, NULL, ?)",
                        messageId, attachment.partIndex(), attachment.filename(), attachment.contentType(),
                        attachment.size(), provider, attachment.contentId()
                );
            } else {
                jdbcTemplate.update(
                        "UPDATE mail_attachments SET filename = ?, content_type = ?, size = ?, content_id = ?, " +
                                "storage_provider = ? WHERE id = ?",
                        attachment.filename(), attachment.contentType(), attachment.size(), attachment.contentId(),
                        provider, existingId
                );
            }
        }
    }

    public List<MailAttachment> findByMessageAndUser(long messageId, long userId) {
        return jdbcTemplate.query(
                "SELECT a.* FROM mail_attachments a JOIN mail_messages m ON m.id = a.message_id " +
                        "JOIN mail_accounts acc ON acc.id = m.account_id WHERE a.message_id = ? AND acc.user_id = ? " +
                        "ORDER BY a.part_index ASC",
                MAPPER, messageId, userId
        );
    }

    public Optional<AttachmentSource> findSource(long attachmentId, long userId) {
        return jdbcTemplate.query(
                "SELECT a.id, a.message_id, m.account_id, m.folder, m.uid, a.part_index, a.filename, " +
                        "a.content_type, a.size, a.storage_provider, a.storage_key " +
                        "FROM mail_attachments a JOIN mail_messages m ON m.id = a.message_id " +
                        "JOIN mail_accounts acc ON acc.id = m.account_id " +
                        "WHERE a.id = ? AND acc.user_id = ?",
                SOURCE_MAPPER, attachmentId, userId
        ).stream().findFirst();
    }

    public void updateStored(long attachmentId, String provider, String key, long size, String checksum) {
        jdbcTemplate.update(
                "UPDATE mail_attachments SET storage_provider = ?, storage_key = ?, size = ?, checksum = ? WHERE id = ?",
                provider, key, size, checksum, attachmentId
        );
    }

    public void deleteByAccount(long accountId) {
        jdbcTemplate.update(
                "DELETE FROM mail_attachments WHERE message_id IN " +
                        "(SELECT id FROM mail_messages WHERE account_id = ?)", accountId
        );
    }

    public void deleteByFolder(long accountId, String folder) {
        jdbcTemplate.update(
                "DELETE a FROM mail_attachments a JOIN mail_messages m ON m.id = a.message_id " +
                        "WHERE m.account_id = ? AND m.folder = ?", accountId, folder
        );
    }
}
