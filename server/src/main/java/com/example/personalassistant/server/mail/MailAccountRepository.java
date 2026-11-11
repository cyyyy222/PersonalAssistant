package com.example.personalassistant.server.mail;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class MailAccountRepository {

    private static final RowMapper<MailAccount> ROW_MAPPER = (rs, rowNum) -> new MailAccount(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("email"),
            rs.getString("display_name"),
            rs.getString("imap_host"),
            rs.getInt("imap_port"),
            rs.getBoolean("imap_ssl"),
            rs.getString("imap_username"),
            rs.getString("smtp_host"),
            rs.getInt("smtp_port"),
            rs.getBoolean("smtp_ssl"),
            rs.getString("secret_cipher"),
            rs.getInt("secret_key_version"),
            rs.getInt("status"),
            rs.getString("archive_folder"),
            rs.getString("sent_folder"),
            rs.getTimestamp("last_sync_at") == null ? null : rs.getTimestamp("last_sync_at").toLocalDateTime(),
            rs.getString("last_error")
    );

    private final JdbcTemplate jdbcTemplate;

    public MailAccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MailAccount> findByUser(long userId) {
        return jdbcTemplate.query(
                "SELECT * FROM mail_accounts WHERE user_id = ? ORDER BY created_at ASC",
                ROW_MAPPER, userId
        );
    }

    public List<MailAccount> findAll() {
        return jdbcTemplate.query("SELECT * FROM mail_accounts ORDER BY id ASC", ROW_MAPPER);
    }

    public void updateFolders(long id, String archiveFolder, String sentFolder) {
        jdbcTemplate.update("UPDATE mail_accounts SET archive_folder = ?, sent_folder = ? WHERE id = ?",
                archiveFolder, sentFolder, id);
    }

    public Optional<MailAccount> findByIdAndUser(long id, long userId) {
        return jdbcTemplate.query(
                "SELECT * FROM mail_accounts WHERE id = ? AND user_id = ?",
                ROW_MAPPER, id, userId
        ).stream().findFirst();
    }

    public Optional<MailAccount> findByUserAndEmail(long userId, String email) {
        return jdbcTemplate.query(
                "SELECT * FROM mail_accounts WHERE user_id = ? AND email = ?",
                ROW_MAPPER, userId, email
        ).stream().findFirst();
    }

    public long insert(long userId, String email, String displayName, MailProvider provider,
                       String secretCipher, int secretKeyVersion) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO mail_accounts (user_id, email, display_name, imap_host, imap_port, imap_ssl, " +
                            "imap_username, smtp_host, smtp_port, smtp_ssl, secret_cipher, secret_key_version) " +
                            "VALUES (?, ?, ?, ?, ?, 1, ?, ?, ?, 1, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, userId);
            statement.setString(2, email);
            statement.setString(3, displayName == null ? "" : displayName);
            statement.setString(4, provider.imapHost());
            statement.setInt(5, provider.imapPort());
            statement.setString(6, email);
            statement.setString(7, provider.smtpHost());
            statement.setInt(8, provider.smtpPort());
            statement.setString(9, secretCipher);
            statement.setInt(10, secretKeyVersion);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("无法创建邮箱账号");
        }
        return key.longValue();
    }

    public void touchSync(long id) {
        jdbcTemplate.update("UPDATE mail_accounts SET last_sync_at = ?, last_error = NULL WHERE id = ?",
                Timestamp.valueOf(java.time.LocalDateTime.now()), id);
    }

    public void updateError(long id, String error) {
        String text = error == null ? null : (error.length() > 500 ? error.substring(0, 500) : error);
        jdbcTemplate.update("UPDATE mail_accounts SET last_error = ? WHERE id = ?", text, id);
    }

    public void delete(long id, long userId) {
        jdbcTemplate.update("DELETE FROM mail_accounts WHERE id = ? AND user_id = ?", id, userId);
    }
}
