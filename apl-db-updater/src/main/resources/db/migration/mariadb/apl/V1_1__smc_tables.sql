/*
 * Copyright (c) 2021. Apollo Foundation.
 */

CREATE TABLE IF NOT EXISTS `smc_contract`
(
    `db_id`          bigint(20) unsigned                     NOT NULL AUTO_INCREMENT,
    `address`        bigint(20)                              NOT NULL,# contract address
    `owner`          bigint(20)                              NOT NULL,# owner
    `transaction_id` bigint(20)                              NOT NULL, # originator, transaction sender (i.e. payer ???)
    `data`           LONGTEXT COLLATE utf8mb4_unicode_ci     NOT NULL,
    `name`           varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
    `args`           TEXT COLLATE utf8mb4_unicode_ci                  DEFAULT NULL,
    `language`       varchar(20) COLLATE utf8mb4_unicode_ci  NOT NULL,
    `version`        varchar(20) COLLATE utf8mb4_unicode_ci  NOT NULL,
    `status`         varchar(20) COLLATE utf8mb4_unicode_ci  NOT NULL,
    `height`         int(11)                                 NOT NULL,
    `latest`         tinyint(1)                              NOT NULL DEFAULT 1,
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
    `deleted` tinyint(1)                             NOT NULL DEFAULT 0,
    UNIQUE KEY `db_id` (`db_id`),
    UNIQUE KEY `smc_contract_address_txid_height_idx` (`address`, `height`),
    KEY `smc_contract_height_address_idx` (`height`, `address`)
) ENGINE = ROCKSDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `smc_mapping`
(
    `db_id`     bigint(20) unsigned                     NOT NULL AUTO_INCREMENT,
    `address`   bigint(20)                              NOT NULL, # contract address
    `entry_key` binary(32)                              NOT NULL, # key
    `name`      varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL, # mapping name
    `object`    LONGTEXT COLLATE utf8mb4_unicode_ci     NOT NULL, # serialized object
    `height`    int(11)                                 NOT NULL,
    `latest`    tinyint(1)                              NOT NULL DEFAULT 1,
    `deleted`   tinyint(1)                              NOT NULL DEFAULT 0,
    UNIQUE KEY `db_id` (`db_id`),
    UNIQUE KEY `smc_mapping_address_key_height_idx` (`address`, `entry_key`, `height`),
    KEY `smc_mapping_height_idx` (`height`)
) ENGINE = ROCKSDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
