/*
 * Copyright (c) 2021. Apollo Foundation.
 */

CREATE TABLE IF NOT EXISTS `smc_contract`
(
    `db_id`                 bigint(20) unsigned                     NOT NULL AUTO_INCREMENT,
    `address`               bigint(20)                              NOT NULL,# contract address
    `owner`                 bigint(20)                              NOT NULL,# owner
    `transaction_id`        bigint(20)                              NOT NULL,# originator, transaction sender (i.e. payer ???)
    `transaction_full_hash` binary(32)                              NOT NULL,
    `fuel_price`            bigint(20)                              NOT NULL,
    `fuel_limit`            bigint(20)                              NOT NULL,
    `fuel_charged`          bigint(20)                              NOT NULL,
    `block_timestamp`       int(11)                                 NOT NULL,
    `data`                  LONGTEXT COLLATE utf8mb4_unicode_ci     NOT NULL,
    `name`                  varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
    `base_contract`         varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,# base contract type, i.e. APL20 ...
    `args`                  TEXT COLLATE utf8mb4_unicode_ci                  DEFAULT NULL,
    `language`              varchar(20) COLLATE utf8mb4_unicode_ci  NOT NULL,
    `version`               varchar(20) COLLATE utf8mb4_unicode_ci  NOT NULL,
    `status`                varchar(20) COLLATE utf8mb4_unicode_ci  NOT NULL,
    `height`                int(11)                                 NOT NULL,
    `latest`                tinyint(1)                              NOT NULL DEFAULT 1,
    UNIQUE KEY `db_id` (`db_id`),
    UNIQUE KEY `smc_contract_address_height_idx` (`address`)
) ENGINE = ROCKSDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `smc_state`
(
    `db_id`   bigint(20) unsigned                    NOT NULL AUTO_INCREMENT,
    `address` bigint(20)                             NOT NULL,
    `object`  LONGTEXT COLLATE utf8mb4_unicode_ci    NOT NULL,
    `status`  varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
    `height`  int(11)                                NOT NULL,
    `latest`  tinyint(1)                             NOT NULL DEFAULT 1,
    UNIQUE KEY `db_id` (`db_id`),
    UNIQUE KEY `smc_state_address_height_idx` (`address`, `height`),
    KEY `smc_state_height_address_idx` (`height`, `address`)
) ENGINE = ROCKSDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `smc_mapping`
(
    `db_id`     bigint(20) unsigned                    NOT NULL AUTO_INCREMENT,
    `address`   bigint(20)                             NOT NULL, # contract address
    `entry_key` binary(32)                             NOT NULL, # key
    `name`      varchar(66) COLLATE utf8mb4_unicode_ci NOT NULL, # mapping name
    `object`    LONGTEXT COLLATE utf8mb4_unicode_ci    NOT NULL, # serialized object
    `height`    int(11)                                NOT NULL,
    `latest`    tinyint(1)                             NOT NULL DEFAULT 1,
    `deleted`   tinyint(1)                             NOT NULL DEFAULT 0,
    UNIQUE KEY `db_id` (`db_id`),
    UNIQUE KEY `smc_mapping_address_name_key_height_idx` (`address`, `name`, `entry_key`, `height`),
    KEY `smc_mapping_height_idx` (`height`)
) ENGINE = ROCKSDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `smc_event`
(
    `db_id`          bigint(20) unsigned                     NOT NULL AUTO_INCREMENT,
    `id`             bigint(20)                              NOT NULL, # event id
    `transaction_id` bigint(20)                              NOT NULL, # transaction id
    `contract`       bigint(20)                              NOT NULL, # contract address
    `signature`      binary(32)                              NOT NULL, # hash(spec+idxCount+anonymous)
    `name`           varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL, # event name
    `spec`           varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL, # event specification(name+params list)
    `idx_count`      tinyint(2)                              NOT NULL DEFAULT 0,
    `is_anonymous`   tinyint(1)                              NOT NULL DEFAULT FALSE,
    `height`         int(11)                                 NOT NULL,
    UNIQUE KEY `db_id` (`db_id`),
    KEY `smc_event_contract_signature_height_idx` (`contract`, `signature`, `height`),
    KEY `smc_event_id_height_idx` (`id`, `height`),
    KEY `smc_event_height_idx` (`height`)
) ENGINE = ROCKSDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `smc_event_log`
(
    `db_id`          bigint(20) unsigned                 NOT NULL AUTO_INCREMENT,
    `event_id`       bigint(20)                          NOT NULL, # event id
    `transaction_id` bigint(20)                          NOT NULL, # transaction id
    `signature`      binary(32)                          NOT NULL, # hash(name+idxCount)
    `state`          LONGTEXT COLLATE utf8mb4_unicode_ci NOT NULL, # serialized object
    `tx_idx`         int(11)                             NOT NULL, # sequence number in transaction, the first is 0
    `height`         int(11)                             NOT NULL,
    UNIQUE KEY `db_id` (`db_id`),
    KEY `smc_event_log_id_height_idx` (`event_id`, `db_id`, `height`),
    KEY `smc_event_log_height_idx` (`height`)
) ENGINE = ROCKSDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
