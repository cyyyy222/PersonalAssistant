package com.example.personalassistant.server.mail;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class MailSyncStateRepository {

    private static final RowMapper<MailSyncState> ROW_MAPPER = (rs, rowNum) -> new MailSyncState(
            rs.getLong("id"),
            rs.getLong("account_id"),
            rs.getString("folder"),
            rs.getLong("uid_validity"),
            rs.getLong("last_uid"),
            rs.getInt("status"),
            rs.getString("error_message")
    );

    private final JdbcTemplate jdbcTemplate;

    public MailSyncStateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<MailSyncState> find(long accountId, String folder) {
        return jdbcTemplate.query(
                "SELECT * FROM mail_sync_state WHERE account_id = ? AND folder = ?",
                ROW_MAPPER, accountId, folder
        ).stream().findFirst();
    }

    public void save(long accountId, String folder, long uidValidity, long lastUid, int status, String error) {
        jdbcTemplate.update(
                "INSERT INTO mail_sync_state (account_id, folder, uid_validity, last_uid, last_sync_at, status, error_message) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE uid_validity = VALUES(uid_validity), last_uid = VALUES(last_uid), " +
                        "last_sync_at = VALUES(last_sync_at), status = VALUES(status), error_message = VALUES(error_message)",
                accountId, folder, uidValidity, lastUid, Timestamp.valueOf(LocalDateTime.now()), status,
                error == null ? null : (error.length() > 500 ? error.substring(0, 500) : error)
        );
    }

    public void deleteByAccount(long accountId) {
        jdbcTemplate.update("DELETE FROM mail_sync_state WHERE account_id = ?", accountId);
    }
}
