/*
 * Copyright (c) 2021. Apollo Foundation.
 */
#(address, data, name, language, fuel, fuel_price, transaction_id, height, latest)
CREATE TABLE IF NOT EXISTS `smc_contract`
(
    `db_id`          bigint(20) unsigned                     NOT NULL AUTO_INCREMENT,
    `address`        bigint(20)                              NOT NULL,
    `data`           LONGTEXT COLLATE utf8mb4_unicode_ci     NOT NULL,
    `name`           varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
    `language`       varchar(10) COLLATE utf8mb4_unicode_ci  NOT NULL,
    `fuel`           bigint(20)                              NOT NULL,
    `fuel_price`     bigint(20)                              NOT NULL,
    `transaction_id` bigint(20)                              NOT NULL,
    `disabled`       tinyint(1)                              NOT NULL DEFAULT 0,
    `height`         int(11)                                 NOT NULL,
    `latest`         tinyint(1)                              NOT NULL DEFAULT 1,
    UNIQUE KEY `db_id` (`db_id`),
    UNIQUE KEY `smc_contract_address_height_idx` (`address`, `height`)
) ENGINE = ROCKSDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

#(address, transaction_id, method, args, object, status, height, latest)
CREATE TABLE IF NOT EXISTS `smc_state`
(
    `db_id`          bigint(20) unsigned                     NOT NULL AUTO_INCREMENT,
    `address`        bigint(20)                              NOT NULL,
    `transaction_id` bigint(20)                              NOT NULL,
    `method`         varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
    `args`           TEXT COLLATE utf8mb4_unicode_ci                  DEFAULT NULL,
    `object`         LONGTEXT COLLATE utf8mb4_unicode_ci     NOT NULL,
    `status`         varchar(20) COLLATE utf8mb4_unicode_ci  NOT NULL,
    `fuel_remaining` bigint(32)                              NOT NULL DEFAULT 0,
    `height`         int(11)                                 NOT NULL,
    `latest`         tinyint(1)                              NOT NULL DEFAULT 1,
    UNIQUE KEY `db_id` (`db_id`),
    UNIQUE KEY `smc_contract_address_height_idx` (`address`, `height`)
) ENGINE = ROCKSDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
