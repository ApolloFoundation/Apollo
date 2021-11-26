/*
 * Copyright (c) 2021. Apollo Foundation.
 */

CREATE TABLE IF NOT EXISTS `smc_contract`
(
    `db_id`                 bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `address`               bigint(20)          NOT NULL,
    `owner`                 bigint(20)          NOT NULL,
    `transaction_id`        bigint(20)          NOT NULL,
    `transaction_full_hash` binary(32)          NOT NULL,
    `fuel_price`            bigint(20)          NOT NULL,
    `fuel_limit`            bigint(20)          NOT NULL,
    `fuel_charged`          bigint(20)          NOT NULL,
    `block_timestamp`       int(11)             NOT NULL,
    `data`                  LONGTEXT            NOT NULL,
    `name`                  varchar(120)        NOT NULL,
    `base_contract`         varchar(120)        NOT NULL,
    `args`                  TEXT                         DEFAULT NULL,
    `language`              varchar(20)         NOT NULL,
    `version`               varchar(20)         NOT NULL,
    `status`                varchar(20)         NOT NULL,
    `height`                int(11)             NOT NULL,
    `latest`                tinyint(1)          NOT NULL DEFAULT 1,
    UNIQUE KEY `smc_contract_address_height_idx` (`address`)
);

CREATE TABLE IF NOT EXISTS `smc_state`
(
    `db_id`   bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `address` bigint(20)  NOT NULL,
    `object`  LONGTEXT    NOT NULL,
    `status`  varchar(20) NOT NULL,
    `height`  int(11)     NOT NULL,
    `latest`  tinyint(1)  NOT NULL DEFAULT 1,
    `deleted` tinyint(1)  NOT NULL DEFAULT 0,
    UNIQUE KEY `smc_state_address_height_idx` (`address`, `height`),
    KEY       `smc_state_height_address_idx` (`height`, `address`)
);

CREATE TABLE IF NOT EXISTS `smc_mapping`
(
    `db_id`     bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `address`   bigint(20)  NOT NULL,
    `entry_key` binary(32)  NOT NULL,
    `name`      varchar(66) NOT NULL,
    `object`    LONGTEXT    NOT NULL,
    `height`    int(11)     NOT NULL,
    `latest`    tinyint(1)  NOT NULL DEFAULT 1,
    `deleted`   tinyint(1)  NOT NULL DEFAULT 0,
    UNIQUE KEY `smc_mapping_address_name_key_height_idx` (`address`, `name`, `entry_key`, `height`),
    KEY         `smc_mapping_height_idx` (`height`)
);

CREATE TABLE IF NOT EXISTS `smc_event`
(
    `db_id`          bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `id`             bigint(20)   NOT NULL,
    `transaction_id` bigint(20)   NOT NULL,
    `contract`       bigint(20)   NOT NULL,
    `signature`      binary(32)   NOT NULL,
    `name`           varchar(128) NOT NULL,
    `spec`           varchar(256) NOT NULL,
    `idx_count`      tinyint(2)   NOT NULL DEFAULT 0,
    `is_anonymous`   tinyint(1)   NOT NULL DEFAULT FALSE,
    `height`         int(11)      NOT NULL,
    KEY              `smc_event_contract_signature_height_idx` (`contract`, `signature`, `height`),
    KEY              `smc_event_id_height_idx` (`id`, `height`),
    KEY              `smc_event_height_idx` (`height`)
);

CREATE TABLE IF NOT EXISTS `smc_event_log`
(
    `db_id`          bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `event_id`       bigint(20) NOT NULL,
    `transaction_id` bigint(20) NOT NULL,
    `signature`      binary(32) NOT NULL,
    `state`          LONGTEXT   NOT NULL,
    `tx_idx`         int(11)    NOT NULL,
    `height`         int(11)    NOT NULL,
    KEY              `smc_event_log_id_height_idx` (`event_id`, `db_id`, `height`),
    KEY              `smc_event_log_height_idx` (`height`)
);
