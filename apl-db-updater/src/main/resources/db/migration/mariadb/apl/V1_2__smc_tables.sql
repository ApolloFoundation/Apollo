/*
 * Copyright (c) 2021. Apollo Foundation.
 */

CREATE TABLE IF NOT EXISTS `smc_event_type`
(
    `db_id`          bigint(20) unsigned                     NOT NULL AUTO_INCREMENT,
    `id`             bigint(20)                              NOT NULL, # event id
    `transaction_id` bigint(20)                              NOT NULL, # transaction id
    `contract`       bigint(20)                              NOT NULL, # contract address
    `signature`      binary(32)                              NOT NULL, # hash(name+idxCount) ???
    `name`           varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL, # event name
    `idx_count`      tinyint(2)                              NOT NULL DEFAULT 0,
    `is_anonymous`   tinyint(1)                              NOT NULL DEFAULT FALSE,
    `height`         int(11)                                 NOT NULL,
    UNIQUE KEY `db_id` (`db_id`),
    UNIQUE KEY `smc_event_log_contract_signature_height_idx` (`contract`, `signature`, `height`),
    KEY `smc_event_log_id_height_idx` (`id`, `height`),
    KEY `smc_event_log_height_idx` (`height`)
) ENGINE = ROCKSDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `smc_event_log`
(
    `db_id`          bigint(20) unsigned                 NOT NULL AUTO_INCREMENT,
    `event_id`       bigint(20)                          NOT NULL, # event id
    `transaction_id` bigint(20)                          NOT NULL, # transaction id
    `signature`      binary(32)                          NOT NULL, # hash(name+idxCount)
    `entry`          LONGTEXT COLLATE utf8mb4_unicode_ci NOT NULL, # serialized object
    `tx_idx`         int(11)                             NOT NULL, # sequence number in transaction, the first is 0
    `height`         int(11)                             NOT NULL,
    UNIQUE KEY `db_id` (`db_id`),
    KEY `smc_event_log_id_idx` (`event_id`, `db_id`),
    KEY `smc_event_log_height_idx` (`height`)
) ENGINE = ROCKSDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
