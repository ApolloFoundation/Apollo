CREATE TABLE IF NOT EXISTS `block`
(
    `db_id`                 bigint(20) unsigned NOT NULL,
    `id`                    bigint(20)          NOT NULL,
    `version`               int(11)             NOT NULL,
    `TIMESTAMP`             int(11)             NOT NULL,
    `previous_block_id`     bigint(20)          DEFAULT NULL,
    `total_amount`          bigint(20)          NOT NULL,
    `total_fee`             bigint(20)          NOT NULL,
    `payload_length`        int(11)             NOT NULL,
    `previous_block_hash`   binary(32)          DEFAULT NULL,
    `cumulative_difficulty` blob                NOT NULL,
    `base_target`           bigint(20)          NOT NULL,
    `next_block_id`         bigint(20)          DEFAULT NULL,
    `height`                int(11)             NOT NULL,
    `generation_signature`  binary(32)          NOT NULL,
    `block_signature`       binary(64)          NOT NULL,
    `payload_hash`          binary(32)          NOT NULL,
    `generator_id`          bigint(20)          NOT NULL,
    `timeout`               int(11)             NOT NULL DEFAULT 0
) ENGINE = ROCKSDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `transaction`
(
    `db_id`                            bigint(20) unsigned NOT NULL,
    `id`                               bigint(20)          NOT NULL,
    `deadline`                         smallint(6)         NOT NULL,
    `recipient_id`                     bigint(20)          DEFAULT NULL,
    `transaction_index`                smallint(6)         NOT NULL,
    `amount`                           bigint(20)          NOT NULL,
    `fee`                              bigint(20)          NOT NULL,
    `full_hash`                        binary(32)          NOT NULL,
    `height`                           int(11)             NOT NULL,
    `block_id`                         bigint(20)          NOT NULL,
    `signature`                        blob                DEFAULT NULL,
    `TIMESTAMP`                        int(11)             NOT NULL,
    `type`                             tinyint(4)          NOT NULL,
    `subtype`                          tinyint(4)          NOT NULL,
    `sender_id`                        bigint(20)          NOT NULL,
    `sender_public_key`                binary(32)          DEFAULT NULL,
    `block_timestamp`                  int(11)             NOT NULL,
    `referenced_transaction_full_hash` binary(32)          DEFAULT NULL,
    `phased`                           tinyint(1)          NOT NULL DEFAULT 0,
    `attachment_bytes`                 blob                DEFAULT NULL,
    `version`                          tinyint(4)          NOT NULL,
    `has_message`                      tinyint(1)          NOT NULL DEFAULT 0,
    `has_encrypted_message`            tinyint(1)          NOT NULL DEFAULT 0,
    `has_public_key_announcement`      tinyint(1)          NOT NULL DEFAULT 0,
    `ec_block_height`                  int(11)             DEFAULT NULL,
    `ec_block_id`                      bigint(20)          DEFAULT NULL,
    `has_encrypttoself_message`        tinyint(1)          NOT NULL DEFAULT 0,
    `has_prunable_message`             tinyint(1)          NOT NULL DEFAULT 0,
    `has_prunable_encrypted_message`   tinyint(1)          NOT NULL DEFAULT 0,
    `has_prunable_attachment`          tinyint(1)          NOT NULL DEFAULT 0
) ENGINE = ROCKSDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS option
(
    name    VARCHAR(100) not null,
    `VALUE` VARCHAR(150)
) ENGINE = ROCKSDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

CREATE UNIQUE INDEX IF NOT EXISTS option_name_value_idx ON option (name, `VALUE`);