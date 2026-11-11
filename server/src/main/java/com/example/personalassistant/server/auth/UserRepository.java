package com.example.personalassistant.server.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepository {

    private static final RowMapper<User> ROW_MAPPER = (resultSet, rowNum) -> new User(
            resultSet.getLong("id"),
            resultSet.getString("username"),
            resultSet.getString("phone"),
            resultSet.getString("password_hash"),
            resultSet.getString("display_name"),
            resultSet.getInt("status")
    );

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findByAccount(String account) {
        return jdbcTemplate.query(
                "SELECT id, username, phone, password_hash, display_name, status FROM users WHERE username = ? OR phone = ? LIMIT 1",
                ROW_MAPPER, account, account
        ).stream().findFirst();
    }

    public Optional<User> findById(long id) {
        return jdbcTemplate.query(
                "SELECT id, username, phone, password_hash, display_name, status FROM users WHERE id = ?",
                ROW_MAPPER, id
        ).stream().findFirst();
    }

    public boolean existsByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?", Integer.class, username
        );
        return count != null && count > 0;
    }

    public void insert(String username, String phone, String passwordHash, String displayName) {
        jdbcTemplate.update(
                "INSERT INTO users (username, phone, password_hash, display_name) VALUES (?, ?, ?, ?)",
                username, phone, passwordHash, displayName
        );
    }
}
