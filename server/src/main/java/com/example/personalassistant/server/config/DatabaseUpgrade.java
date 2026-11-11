package com.example.personalassistant.server.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseUpgrade implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseUpgrade(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        addColumn("mail_accounts", "archive_folder", "VARCHAR(64) NULL");
        addColumn("mail_accounts", "sent_folder", "VARCHAR(64) NULL");
        addColumn("mail_messages", "is_archived", "TINYINT NOT NULL DEFAULT 0");
    }

    private void addColumn(String table, String column, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class, table, column
        );
        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }
}
