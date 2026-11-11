CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_phone (phone)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS mail_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    display_name VARCHAR(128) NOT NULL DEFAULT '',
    imap_host VARCHAR(128) NOT NULL,
    imap_port INT NOT NULL DEFAULT 993,
    imap_ssl TINYINT NOT NULL DEFAULT 1,
    imap_username VARCHAR(255) NOT NULL,
    smtp_host VARCHAR(128) NOT NULL,
    smtp_port INT NOT NULL DEFAULT 465,
    smtp_ssl TINYINT NOT NULL DEFAULT 1,
    secret_cipher TEXT NOT NULL,
    secret_key_version INT NOT NULL DEFAULT 1,
    status TINYINT NOT NULL DEFAULT 1,
    archive_folder VARCHAR(64) NULL,
    sent_folder VARCHAR(64) NULL,
    last_sync_at DATETIME NULL,
    last_error VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mail_account_user_email (user_id, email),
    KEY idx_mail_account_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mail_sync_state (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    folder VARCHAR(64) NOT NULL,
    uid_validity BIGINT NOT NULL DEFAULT 0,
    last_uid BIGINT NOT NULL DEFAULT 0,
    last_sync_at DATETIME NULL,
    status TINYINT NOT NULL DEFAULT 0,
    error_message VARCHAR(512) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mail_sync_account_folder (account_id, folder)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mail_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    folder VARCHAR(64) NOT NULL DEFAULT 'INBOX',
    uid BIGINT NOT NULL,
    message_id VARCHAR(512) NULL,
    subject VARCHAR(512) NOT NULL DEFAULT '',
    from_name VARCHAR(255) NOT NULL DEFAULT '',
    from_address VARCHAR(255) NOT NULL DEFAULT '',
    to_addresses TEXT NULL,
    sent_at DATETIME NULL,
    received_at DATETIME NULL,
    snippet VARCHAR(512) NOT NULL DEFAULT '',
    body_text MEDIUMTEXT NULL,
    body_html MEDIUMTEXT NULL,
    has_attachments TINYINT NOT NULL DEFAULT 0,
    attachment_count INT NOT NULL DEFAULT 0,
    is_read TINYINT NOT NULL DEFAULT 0,
    is_starred TINYINT NOT NULL DEFAULT 0,
    is_archived TINYINT NOT NULL DEFAULT 0,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    raw_size INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mail_message_uid (account_id, folder, uid),
    KEY idx_mail_message_list (account_id, folder, sent_at),
    KEY idx_mail_message_unread (account_id, folder, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mail_attachments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    message_id BIGINT NOT NULL,
    part_index INT NOT NULL DEFAULT 0,
    filename VARCHAR(255) NOT NULL DEFAULT '',
    content_type VARCHAR(128) NOT NULL DEFAULT 'application/octet-stream',
    size BIGINT NOT NULL DEFAULT 0,
    storage_provider VARCHAR(32) NOT NULL DEFAULT 'local',
    storage_key VARCHAR(512) NULL,
    content_id VARCHAR(255) NULL,
    checksum VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mail_attachment_part (message_id, part_index),
    KEY idx_mail_attachment_message (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
