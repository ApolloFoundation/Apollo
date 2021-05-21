/*
 * Copyright (c) 2021. Apollo Foundation.
 */

CREATE TABLE IF NOT EXISTS `smc_mapping`
(
    `db_id`   bigint(20) unsigned                     NOT NULL AUTO_INCREMENT,
    `address` bigint(20)                              NOT NULL,# contract address
    `key`     tinyblob                                NOT NULL,# key
    `name`    varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,# mapping name
    `object`  LONGTEXT COLLATE utf8mb4_unicode_ci     NOT NULL,# serialized object
    `height`  int(11)                                 NOT NULL,
    `latest`  tinyint(1)                              NOT NULL DEFAULT 1,
    UNIQUE KEY `db_id` (`db_id`),
    UNIQUE KEY `smc_mapping_address_key_height_idx` (`address`, `key`, `height`),
    KEY `smc_mapping_height_idx` (`height`)
) ENGINE = ROCKSDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
