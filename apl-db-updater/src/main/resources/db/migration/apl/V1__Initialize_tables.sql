


CREATE TABLE `account` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `balance` bigint(20) NOT NULL,
  `unconfirmed_balance` bigint(20) NOT NULL,
  `has_control_phasing` tinyint(1) NOT NULL DEFAULT 0,
  `forged_balance` bigint(20) NOT NULL,
  `active_lessee_id` bigint(20) DEFAULT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  `parent` bigint(20) DEFAULT NULL,
  `is_multi_sig` tinyint(1) NOT NULL DEFAULT 0,
  `addr_scope` tinyint(4) NOT NULL DEFAULT 0,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `account_id_height_idx` (`id`,`height`),
  KEY `account_active_lessee_id_idx` (`active_lessee_id`),
  KEY `account_height_id_idx` (`height`,`id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `account_asset` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `account_id` bigint(20) NOT NULL,
  `asset_id` bigint(20) NOT NULL,
  `quantity` bigint(20) NOT NULL,
  `unconfirmed_quantity` bigint(20) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `account_asset_id_height_idx` (`account_id`,`asset_id`,`height`),
  KEY `account_asset_quantity_idx` (`quantity`),
  KEY `account_asset_asset_id_idx` (`asset_id`),
  KEY `account_asset_height_id_idx` (`height`,`account_id`,`asset_id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `account_control_phasing` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `account_id` bigint(20) NOT NULL,
  `whitelist` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`whitelist`)),
  `voting_model` tinyint(4) NOT NULL,
  `quorum` bigint(20) DEFAULT NULL,
  `min_balance` bigint(20) DEFAULT NULL,
  `holding_id` bigint(20) DEFAULT NULL,
  `min_balance_model` tinyint(4) DEFAULT NULL,
  `max_fees` bigint(20) DEFAULT NULL,
  `min_duration` smallint(6) DEFAULT NULL,
  `max_duration` smallint(6) DEFAULT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `account_control_phasing_id_height_idx` (`account_id`,`height`),
  KEY `account_control_phasing_height_id_idx` (`height`,`account_id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `account_currency` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `account_id` bigint(20) NOT NULL,
  `currency_id` bigint(20) NOT NULL,
  `units` bigint(20) NOT NULL,
  `unconfirmed_units` bigint(20) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `account_currency_id_height_idx` (`account_id`,`currency_id`,`height`),
  KEY `account_currency_units_idx` (`units`),
  KEY `account_currency_currency_id_idx` (`currency_id`),
  KEY `account_currency_height_id_idx` (`height`,`account_id`,`currency_id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `account_guaranteed_balance` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `account_id` bigint(20) NOT NULL,
  `additions` bigint(20) NOT NULL,
  `height` int(11) NOT NULL,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `account_guaranteed_balance_id_height_idx` (`account_id`,`height`),
  KEY `account_guaranteed_balance_height_idx` (`height`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `account_info` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `account_id` bigint(20) NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `account_info_id_height_idx` (`account_id`,`height`),
  KEY `account_info_height_id_idx` (`height`,`account_id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `account_lease` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `lessor_id` bigint(20) NOT NULL,
  `current_leasing_height_from` int(11) DEFAULT NULL,
  `current_leasing_height_to` int(11) DEFAULT NULL,
  `current_lessee_id` bigint(20) DEFAULT NULL,
  `next_leasing_height_from` int(11) DEFAULT NULL,
  `next_leasing_height_to` int(11) DEFAULT NULL,
  `next_lessee_id` bigint(20) DEFAULT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `account_lease_lessor_id_height_idx` (`lessor_id`,`height`),
  KEY `account_lease_current_leasing_height_from_idx` (`current_leasing_height_from`),
  KEY `account_lease_current_leasing_height_to_idx` (`current_leasing_height_to`),
  KEY `account_lease_height_id_idx` (`height`,`lessor_id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `account_ledger` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `account_id` bigint(20) NOT NULL,
  `event_type` tinyint(4) NOT NULL,
  `event_id` bigint(20) NOT NULL,
  `holding_type` tinyint(4) NOT NULL,
  `holding_id` bigint(20) DEFAULT NULL,
  `change` bigint(20) NOT NULL,
  `balance` bigint(20) NOT NULL,
  `block_id` bigint(20) NOT NULL,
  `height` int(11) NOT NULL,
  `TIMESTAMP` int(11) NOT NULL,
  UNIQUE KEY `db_id` (`db_id`),
  KEY `account_ledger_id_idx` (`account_id`,`db_id`),
  KEY `account_ledger_height_idx` (`height`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `account_property` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `recipient_id` bigint(20) NOT NULL,
  `setter_id` bigint(20) DEFAULT NULL,
  `property` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `VALUE` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `account_property_id_height_idx` (`id`,`height`),
  KEY `account_property_height_id_idx` (`height`,`id`),
  KEY `account_property_recipient_height_idx` (`recipient_id`,`height`),
  KEY `account_property_setter_recipient_idx` (`setter_id`,`recipient_id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `alias` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `alias_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `alias_name_lower` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `alias_uri` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `TIMESTAMP` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `alias_id_height_idx` (`id`,`height`),
  KEY `alias_account_id_idx` (`account_id`,`height`),
  KEY `alias_name_lower_idx` (`alias_name_lower`(191)),
  KEY `alias_height_id_idx` (`height`,`id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `alias_offer` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `price` bigint(20) NOT NULL,
  `buyer_id` bigint(20) DEFAULT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `alias_offer_id_height_idx` (`id`,`height`),
  KEY `alias_offer_height_id_idx` (`height`,`id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `ask_order` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `asset_id` bigint(20) NOT NULL,
  `price` bigint(20) NOT NULL,
  `transaction_index` smallint(6) NOT NULL,
  `transaction_height` int(11) NOT NULL,
  `quantity` bigint(20) NOT NULL,
  `creation_height` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `ask_order_id_height_idx` (`id`,`height`),
  KEY `ask_order_account_id_idx` (`account_id`,`height`),
  KEY `ask_order_asset_id_price_idx` (`asset_id`,`price`),
  KEY `ask_order_creation_idx` (`creation_height`),
  KEY `ask_order_height_id_idx` (`height`,`id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `asset` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `quantity` bigint(20) NOT NULL,
  `decimals` tinyint(4) NOT NULL,
  `initial_quantity` bigint(20) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `asset_id_height_idx` (`id`,`height`),
  KEY `asset_account_id_idx` (`account_id`),
  KEY `asset_height_id_idx` (`height`,`id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `asset_delete` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `asset_id` bigint(20) NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `quantity` bigint(20) NOT NULL,
  `TIMESTAMP` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `asset_delete_id_idx` (`id`),
  KEY `asset_delete_asset_id_idx` (`asset_id`,`height`),
  KEY `asset_delete_account_id_idx` (`account_id`,`height`),
  KEY `asset_delete_height_idx` (`height`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `asset_dividend` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `asset_id` bigint(20) NOT NULL,
  `amount` bigint(20) NOT NULL,
  `dividend_height` int(11) NOT NULL,
  `total_dividend` bigint(20) NOT NULL,
  `num_accounts` bigint(20) NOT NULL,
  `TIMESTAMP` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `asset_dividend_id_idx` (`id`),
  KEY `asset_dividend_asset_id_idx` (`asset_id`,`height`),
  KEY `asset_dividend_height_idx` (`height`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `asset_transfer` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `asset_id` bigint(20) NOT NULL,
  `sender_id` bigint(20) NOT NULL,
  `recipient_id` bigint(20) NOT NULL,
  `quantity` bigint(20) NOT NULL,
  `TIMESTAMP` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `asset_transfer_id_idx` (`id`),
  KEY `asset_transfer_asset_id_idx` (`asset_id`,`height`),
  KEY `asset_transfer_sender_id_idx` (`sender_id`,`height`),
  KEY `asset_transfer_recipient_id_idx` (`recipient_id`,`height`),
  KEY `asset_transfer_height_idx` (`height`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `bid_order` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `asset_id` bigint(20) NOT NULL,
  `price` bigint(20) NOT NULL,
  `transaction_index` smallint(6) NOT NULL,
  `transaction_height` int(11) NOT NULL,
  `quantity` bigint(20) NOT NULL,
  `creation_height` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `bid_order_id_height_idx` (`id`,`height`),
  KEY `bid_order_account_id_idx` (`account_id`,`height`),
  KEY `bid_order_asset_id_price_idx` (`asset_id`,`price`),
  KEY `bid_order_creation_idx` (`creation_height`),
  KEY `bid_order_height_id_idx` (`height`,`id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `block` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `version` int(11) NOT NULL,
  `TIMESTAMP` int(11) NOT NULL,
  `previous_block_id` bigint(20) DEFAULT NULL,
  `total_amount` bigint(20) NOT NULL,
  `total_fee` bigint(20) NOT NULL,
  `payload_length` int(11) NOT NULL,
  `previous_block_hash` binary(32) DEFAULT NULL,
  `cumulative_difficulty` blob NOT NULL,
  `base_target` bigint(20) NOT NULL,
  `next_block_id` bigint(20) DEFAULT NULL,
  `height` int(11) NOT NULL,
  `generation_signature` binary(32) NOT NULL,
  `block_signature` binary(64) NOT NULL,
  `payload_hash` binary(32) NOT NULL,
  `generator_id` bigint(20) NOT NULL,
  `timeout` int(11) NOT NULL DEFAULT 0,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `block_id_idx` (`id`),
  UNIQUE KEY `block_height_idx` (`height`),
  UNIQUE KEY `block_timestamp_idx` (`TIMESTAMP`),
  KEY `block_generator_id_idx` (`generator_id`),
  CONSTRAINT `chk_timeout` CHECK (`timeout` >= 0)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `block_index` (
  `block_id` bigint(20) NOT NULL,
  `block_height` int(11) NOT NULL,
  UNIQUE KEY `block_index_block_id_idx` (`block_id`),
  UNIQUE KEY `block_index_block_height_idx` (`block_height`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `buy_offer` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `currency_id` bigint(20) NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `rate` bigint(20) NOT NULL,
  `unit_limit` bigint(20) NOT NULL,
  `supply` bigint(20) NOT NULL,
  `expiration_height` int(11) NOT NULL,
  `creation_height` int(11) NOT NULL,
  `transaction_index` smallint(6) NOT NULL,
  `transaction_height` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `buy_offer_id_idx` (`id`,`height`),
  KEY `buy_offer_currency_id_account_id_idx` (`currency_id`,`account_id`,`height`),
  KEY `buy_offer_rate_height_idx` (`rate`,`creation_height`),
  KEY `buy_offer_height_id_idx` (`height`,`id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `currency` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name_lower` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `code` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `type` int(11) NOT NULL,
  `initial_supply` bigint(20) NOT NULL DEFAULT 0,
  `reserve_supply` bigint(20) NOT NULL,
  `max_supply` bigint(20) NOT NULL,
  `creation_height` int(11) NOT NULL,
  `issuance_height` int(11) NOT NULL,
  `min_reserve_per_unit_atm` bigint(20) NOT NULL,
  `min_difficulty` tinyint(4) NOT NULL,
  `max_difficulty` tinyint(4) NOT NULL,
  `ruleset` tinyint(4) NOT NULL,
  `algorithm` tinyint(4) NOT NULL,
  `decimals` tinyint(4) NOT NULL DEFAULT 0,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `currency_id_height_idx` (`id`,`height`),
  KEY `currency_account_id_idx` (`account_id`),
  KEY `currency_name_idx` (`name_lower`(191),`height`),
  KEY `currency_code_idx` (`code`(191),`height`),
  KEY `currency_creation_height_idx` (`creation_height`),
  KEY `currency_issuance_height_idx` (`issuance_height`),
  KEY `currency_height_id_idx` (`height`,`id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `currency_founder` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `currency_id` bigint(20) NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `amount` bigint(20) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `currency_founder_currency_id_idx` (`currency_id`,`account_id`,`height`),
  KEY `currency_founder_account_id_idx` (`account_id`,`height`),
  KEY `currency_founder_height_id_idx` (`height`,`currency_id`,`account_id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `currency_mint` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `currency_id` bigint(20) NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `counter` bigint(20) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `currency_mint_currency_id_account_id_idx` (`currency_id`,`account_id`,`height`),
  KEY `currency_mint_height_id_idx` (`height`,`currency_id`,`account_id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `currency_supply` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `current_supply` bigint(20) NOT NULL,
  `current_reserve_per_unit_atm` bigint(20) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `currency_supply_id_height_idx` (`id`,`height`),
  KEY `currency_supply_height_id_idx` (`height`,`id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `currency_transfer` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `currency_id` bigint(20) NOT NULL,
  `sender_id` bigint(20) NOT NULL,
  `recipient_id` bigint(20) NOT NULL,
  `units` bigint(20) NOT NULL,
  `TIMESTAMP` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `currency_transfer_id_idx` (`id`),
  KEY `currency_transfer_currency_id_idx` (`currency_id`,`height`),
  KEY `currency_transfer_sender_id_idx` (`sender_id`,`height`),
  KEY `currency_transfer_recipient_id_idx` (`recipient_id`,`height`),
  KEY `currency_transfer_height_idx` (`height`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `data_tag` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `tag` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tag_count` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `data_tag_tag_height_idx` (`tag`,`height`),
  KEY `data_tag_count_height_idx` (`tag_count`,`height`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `dex_candlestick` (
  `coin` tinyint(4) NOT NULL,
  `open` decimal(12,7) NOT NULL,
  `close` decimal(12,7) NOT NULL,
  `min` decimal(12,7) NOT NULL,
  `max` decimal(12,7) NOT NULL,
  `from_volume` decimal(12,2) NOT NULL,
  `to_volume` decimal(12,7) NOT NULL,
  `timestamp` int(11) NOT NULL,
  `open_order_timestamp` int(11) NOT NULL,
  `close_order_timestamp` int(11) NOT NULL,
  UNIQUE KEY `dex_candlestick_coin_timestamp_idx` (`coin`,`timestamp`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `dex_contract` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `offer_id` bigint(20) NOT NULL,
  `counter_offer_id` bigint(20) NOT NULL,
  `secret_hash` binary(32) DEFAULT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  `deadline_to_reply` int(11) NOT NULL,
  `status` tinyint(4) NOT NULL,
  `sender` bigint(20) NOT NULL,
  `recipient` bigint(20) NOT NULL,
  `encrypted_secret` binary(64) DEFAULT NULL,
  `transfer_tx_id` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `counter_transfer_tx_id` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `dex_contract_id_height_idx` (`id`,`height`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `dex_offer` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `type` tinyint(4) NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `offer_currency` tinyint(4) NOT NULL,
  `offer_amount` bigint(20) NOT NULL,
  `pair_currency` tinyint(4) NOT NULL,
  `pair_rate` bigint(20) NOT NULL,
  `finish_time` int(11) NOT NULL,
  `status` tinyint(4) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  `from_address` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `to_address` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `dex_offer_id_height_idx` (`id`,`height`),
  KEY `dex_offer_overdue_idx` (`status`,`finish_time`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `dex_operation` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `account` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `stage` tinyint(4) NOT NULL,
  `eid` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `details` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `finished` tinyint(1) NOT NULL DEFAULT 0,
  `ts` timestamp(4) NOT NULL DEFAULT current_timestamp(4) ON UPDATE current_timestamp(4),
  UNIQUE KEY `db_id` (`db_id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `dex_transaction` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `hash` blob NOT NULL,
  `tx` blob NOT NULL,
  `operation` tinyint(4) NOT NULL,
  `params` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `account` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `timestamp` bigint(20) DEFAULT NULL,
  UNIQUE KEY `db_id` (`db_id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `exchange` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `transaction_id` bigint(20) NOT NULL,
  `currency_id` bigint(20) NOT NULL,
  `block_id` bigint(20) NOT NULL,
  `offer_id` bigint(20) NOT NULL,
  `seller_id` bigint(20) NOT NULL,
  `buyer_id` bigint(20) NOT NULL,
  `units` bigint(20) NOT NULL,
  `rate` bigint(20) NOT NULL,
  `TIMESTAMP` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `exchange_offer_idx` (`transaction_id`,`offer_id`),
  KEY `exchange_currency_id_idx` (`currency_id`,`height`),
  KEY `exchange_seller_id_idx` (`seller_id`,`height`),
  KEY `exchange_buyer_id_idx` (`buyer_id`,`height`),
  KEY `exchange_height_idx` (`height`),
  KEY `exchange_height_db_id_idx` (`height`,`db_id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `exchange_request` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `currency_id` bigint(20) NOT NULL,
  `units` bigint(20) NOT NULL,
  `rate` bigint(20) NOT NULL,
  `is_buy` tinyint(1) NOT NULL,
  `TIMESTAMP` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `exchange_request_id_idx` (`id`),
  KEY `exchange_request_account_currency_idx` (`account_id`,`currency_id`,`height`),
  KEY `exchange_request_currency_idx` (`currency_id`,`height`),
  KEY `exchange_request_height_db_id_idx` (`height`,`db_id`),
  KEY `exchange_request_height_idx` (`height`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `genesis_public_key` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `account_id` bigint(20) NOT NULL,
  `public_key` binary(32) DEFAULT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `genesis_public_key_account_id_height_idx` (`account_id`,`height`),
  KEY `genesis_public_key_height_idx` (`height`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `goods` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `seller_id` bigint(20) NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `parsed_tags` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`parsed_tags`)),
  `has_image` tinyint(1) NOT NULL,
  `tags` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `TIMESTAMP` int(11) NOT NULL,
  `quantity` int(11) NOT NULL,
  `price` bigint(20) NOT NULL,
  `delisted` tinyint(1) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `goods_id_height_idx` (`id`,`height`),
  KEY `goods_seller_id_name_idx` (`seller_id`,`name`(191)),
  KEY `goods_timestamp_idx` (`TIMESTAMP`,`height`),
  KEY `goods_height_id_idx` (`height`,`id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `mandatory_transaction` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `transaction_bytes` blob NOT NULL,
  `required_tx_hash` binary(32) DEFAULT NULL,
  UNIQUE KEY `db_id` (`db_id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `option` (
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `VALUE` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  UNIQUE KEY `option_name_value_idx` (`name`,`VALUE`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `order_scan` (
  `coin` tinyint(4) NOT NULL,
  `last_db_id` bigint(20) NOT NULL,
  UNIQUE KEY `order_scan_coin_idx` (`coin`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `peer` (
  `address` varchar(191) COLLATE utf8mb4_unicode_ci NOT NULL,
  `last_updated` int(11) DEFAULT NULL,
  `services` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`address`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `phasing_approval_tx` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `phasing_tx` bigint(20) NOT NULL,
  `approved_tx` bigint(20) NOT NULL,
  `height` int(11) NOT NULL,
  UNIQUE KEY `db_id` (`db_id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `phasing_poll` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `whitelist_size` tinyint(4) NOT NULL DEFAULT 0,
  `finish_height` int(11) NOT NULL,
  `voting_model` tinyint(4) NOT NULL,
  `quorum` bigint(20) DEFAULT NULL,
  `min_balance` bigint(20) DEFAULT NULL,
  `holding_id` bigint(20) DEFAULT NULL,
  `min_balance_model` tinyint(4) DEFAULT NULL,
  `hashed_secret` blob DEFAULT NULL,
  `algorithm` tinyint(4) DEFAULT NULL,
  `height` int(11) NOT NULL,
  `finish_time` int(11) NOT NULL DEFAULT -1,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `phasing_poll_id_idx` (`id`),
  KEY `phasing_poll_height_idx` (`height`),
  KEY `phasing_poll_account_id_idx` (`account_id`,`height`),
  KEY `phasing_poll_holding_id_idx` (`holding_id`,`height`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `phasing_poll_linked_transaction` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `transaction_id` bigint(20) NOT NULL,
  `linked_full_hash` binary(32) NOT NULL,
  `linked_transaction_id` bigint(20) NOT NULL,
  `height` int(11) NOT NULL,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `phasing_poll_linked_transaction_id_link_idx` (`transaction_id`,`linked_transaction_id`),
  UNIQUE KEY `phasing_poll_linked_transaction_link_id_idx` (`linked_transaction_id`,`transaction_id`),
  KEY `phasing_poll_linked_transaction_height_idx` (`height`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `phasing_poll_result` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `result` bigint(20) NOT NULL,
  `approved` tinyint(1) NOT NULL,
  `height` int(11) NOT NULL,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `phasing_poll_result_id_idx` (`id`),
  KEY `phasing_poll_result_height_idx` (`height`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `phasing_poll_voter` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `transaction_id` bigint(20) NOT NULL,
  `voter_id` bigint(20) NOT NULL,
  `height` int(11) NOT NULL,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `phasing_poll_voter_transaction_voter_idx` (`transaction_id`,`voter_id`),
  KEY `phasing_poll_voter_height_idx` (`height`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `phasing_vote` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vote_id` bigint(20) NOT NULL,
  `transaction_id` bigint(20) NOT NULL,
  `voter_id` bigint(20) NOT NULL,
  `height` int(11) NOT NULL,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `phasing_vote_transaction_voter_idx` (`transaction_id`,`voter_id`),
  KEY `phasing_vote_height_idx` (`height`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `poll` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `options` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`options`)),
  `min_num_options` tinyint(4) DEFAULT NULL,
  `max_num_options` tinyint(4) DEFAULT NULL,
  `min_range_value` tinyint(4) DEFAULT NULL,
  `max_range_value` tinyint(4) DEFAULT NULL,
  `TIMESTAMP` int(11) NOT NULL,
  `finish_height` int(11) NOT NULL,
  `voting_model` tinyint(4) NOT NULL,
  `min_balance` bigint(20) DEFAULT NULL,
  `min_balance_model` tinyint(4) DEFAULT NULL,
  `holding_id` bigint(20) DEFAULT NULL,
  `height` int(11) NOT NULL,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `poll_id_idx` (`id`),
  KEY `poll_height_idx` (`height`),
  KEY `poll_account_idx` (`account_id`),
  KEY `poll_finish_height_idx` (`finish_height`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `poll_result` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `poll_id` bigint(20) NOT NULL,
  `result` bigint(20) DEFAULT NULL,
  `weight` bigint(20) NOT NULL,
  `height` int(11) NOT NULL,
  UNIQUE KEY `db_id` (`db_id`),
  KEY `poll_result_poll_id_idx` (`poll_id`),
  KEY `poll_result_height_idx` (`height`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `prunable_message` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `sender_id` bigint(20) NOT NULL,
  `recipient_id` bigint(20) DEFAULT NULL,
  `message` blob DEFAULT NULL,
  `message_is_text` tinyint(1) NOT NULL,
  `is_compressed` tinyint(1) NOT NULL,
  `encrypted_message` blob DEFAULT NULL,
  `encrypted_is_text` tinyint(1) DEFAULT 0,
  `block_timestamp` int(11) NOT NULL,
  `transaction_timestamp` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `prunable_message_id_idx` (`id`),
  KEY `prunable_message_transaction_timestamp_idx` (`transaction_timestamp`),
  KEY `prunable_message_sender_idx` (`sender_id`),
  KEY `prunable_message_recipient_idx` (`recipient_id`),
  KEY `prunable_message_block_timestamp_dbid_idx` (`block_timestamp`,`db_id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `public_key` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `account_id` bigint(20) NOT NULL,
  `public_key` binary(32) DEFAULT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `public_key_account_id_height_idx` (`account_id`,`height`),
  KEY `public_key_height_idx` (`height`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `purchase` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `buyer_id` bigint(20) NOT NULL,
  `goods_id` bigint(20) NOT NULL,
  `seller_id` bigint(20) NOT NULL,
  `quantity` int(11) NOT NULL,
  `price` bigint(20) NOT NULL,
  `deadline` int(11) NOT NULL,
  `note` blob DEFAULT NULL,
  `nonce` binary(32) DEFAULT NULL,
  `TIMESTAMP` int(11) NOT NULL,
  `pending` tinyint(1) NOT NULL,
  `goods` blob DEFAULT NULL,
  `goods_nonce` binary(32) DEFAULT NULL,
  `goods_is_text` tinyint(1) NOT NULL DEFAULT 1,
  `refund_note` blob DEFAULT NULL,
  `refund_nonce` binary(32) DEFAULT NULL,
  `has_feedback_notes` tinyint(1) NOT NULL DEFAULT 0,
  `has_public_feedbacks` tinyint(1) NOT NULL DEFAULT 0,
  `discount` bigint(20) NOT NULL,
  `refund` bigint(20) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `purchase_id_height_idx` (`id`,`height`),
  KEY `purchase_buyer_id_height_idx` (`buyer_id`,`height`),
  KEY `purchase_seller_id_height_idx` (`seller_id`,`height`),
  KEY `purchase_deadline_idx` (`deadline`,`height`),
  KEY `purchase_timestamp_idx` (`TIMESTAMP`,`id`),
  KEY `purchase_height_id_idx` (`height`,`id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `purchase_feedback` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `feedback_data` blob NOT NULL,
  `feedback_nonce` binary(32) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  UNIQUE KEY `db_id` (`db_id`),
  KEY `purchase_feedback_id_height_idx` (`id`,`height`),
  KEY `purchase_feedback_height_id_idx` (`height`,`id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `purchase_public_feedback` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `public_feedback` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  UNIQUE KEY `db_id` (`db_id`),
  KEY `purchase_public_feedback_id_height_idx` (`id`,`height`),
  KEY `purchase_public_feedback_height_id_idx` (`height`,`id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `referenced_transaction` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `transaction_id` bigint(20) NOT NULL,
  `referenced_transaction_id` bigint(20) NOT NULL,
  `height` int(11) NOT NULL DEFAULT -1,
  UNIQUE KEY `db_id` (`db_id`),
  KEY `referenced_transaction_referenced_transaction_id_idx` (`referenced_transaction_id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `scan` (
  `rescan` tinyint(1) NOT NULL DEFAULT 0,
  `height` int(11) NOT NULL DEFAULT 0,
  `validate` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `sell_offer` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `currency_id` bigint(20) NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `rate` bigint(20) NOT NULL,
  `unit_limit` bigint(20) NOT NULL,
  `supply` bigint(20) NOT NULL,
  `expiration_height` int(11) NOT NULL,
  `creation_height` int(11) NOT NULL,
  `transaction_index` smallint(6) NOT NULL,
  `transaction_height` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `sell_offer_id_idx` (`id`,`height`),
  KEY `sell_offer_currency_id_account_id_idx` (`currency_id`,`account_id`,`height`),
  KEY `sell_offer_rate_height_idx` (`rate`,`creation_height`),
  KEY `sell_offer_height_id_idx` (`height`,`id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `shard` (
  `shard_id` bigint(20) NOT NULL,
  `shard_hash` blob DEFAULT NULL,
  `shard_height` int(11) NOT NULL DEFAULT 0,
  `shard_state` bigint(20) DEFAULT 0,
  `zip_hash_crc` blob DEFAULT NULL,
  `generator_ids` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`generator_ids`)),
  `block_timeouts` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`block_timeouts`)),
  `block_timestamps` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`block_timestamps`)),
  `prunable_zip_hash` blob DEFAULT NULL,
  PRIMARY KEY (`shard_id`),
  UNIQUE KEY `shard_height_index` (`shard_height`,`shard_id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `shard_recovery` (
  `shard_recovery_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `state` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `object_name` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `column_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_column_value` bigint(20) DEFAULT NULL,
  `processed_object` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `height` int(11) NOT NULL,
  PRIMARY KEY (`shard_recovery_id`),
  UNIQUE KEY `shard_recovery_id` (`shard_recovery_id`),
  UNIQUE KEY `shard_recovery_id_state_object_idx` (`shard_recovery_id`,`state`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `shuffling` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `holding_id` bigint(20) DEFAULT NULL,
  `holding_type` tinyint(4) NOT NULL,
  `issuer_id` bigint(20) NOT NULL,
  `amount` bigint(20) NOT NULL,
  `participant_count` tinyint(4) NOT NULL,
  `blocks_remaining` smallint(6) DEFAULT NULL,
  `stage` tinyint(4) NOT NULL,
  `assignee_account_id` bigint(20) DEFAULT NULL,
  `registrant_count` tinyint(4) NOT NULL,
  `recipient_public_keys` blob DEFAULT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `shuffling_id_height_idx` (`id`,`height`),
  KEY `shuffling_holding_id_height_idx` (`holding_id`,`height`),
  KEY `shuffling_assignee_account_id_height_idx` (`assignee_account_id`,`height`),
  KEY `shuffling_height_id_idx` (`height`,`id`),
  KEY `shuffling_blocks_remaining_height_idx` (`blocks_remaining`,`height`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `shuffling_data` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `shuffling_id` bigint(20) NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`data`)),
  `transaction_timestamp` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `shuffling_data_id_height_idx` (`shuffling_id`,`height`),
  KEY `shuffling_data_transaction_timestamp_idx` (`transaction_timestamp`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `shuffling_participant` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `shuffling_id` bigint(20) NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `next_account_id` bigint(20) DEFAULT NULL,
  `participant_index` tinyint(4) NOT NULL,
  `state` tinyint(4) NOT NULL,
  `blame_data` blob DEFAULT NULL,
  `key_seeds` blob DEFAULT NULL,
  `data_transaction_full_hash` binary(32) DEFAULT NULL,
  `data_hash` binary(32) DEFAULT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `shuffling_participant_shuffling_id_account_id_idx` (`shuffling_id`,`account_id`,`height`),
  KEY `shuffling_participant_height_idx` (`height`,`shuffling_id`,`account_id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `smc_contract`
(
    `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `address` varchar(256) COLLATE latin1_bin NOT NULL,
    `transaction_id` varchar(256) NOT NULL,
    `name` varchar(1024) NOT NULL,
    `data` LONGTEXT COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `language` varchar(64) NOT NULL,
    `fuel` varchar(160) NOT NULL,
    `fuel_price` varchar(160) NOT NULL,
    `height` int(11) NOT NULL,
    `latest` tinyint(1) NOT NULL DEFAULT 1,
    UNIQUE KEY `db_id` (`db_id`),
    UNIQUE KEY `smc_contract_address_idx` (`address`, `height`),
    KEY `smc_contract_height_idx` (`height`, `address`)
) ENGINE = ROCKSDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;




CREATE TABLE `smc_contract_state`
(
    `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `address` varchar(256) COLLATE latin1_bin NOT NULL,
    `transaction_id` varchar(256) COLLATE latin1_bin NOT NULL,
    `method` varchar(1024) NOT NULL,
    `args` TEXT DEFAULT NULL,
    `object` LONGTEXT NOT NULL,
    `status` tinyint(4) NOT NULL,
    `height` int(11) NOT NULL,
    `latest` tinyint(1) NOT NULL DEFAULT 1,
    UNIQUE KEY `db_id` (`db_id`),
    UNIQUE KEY `smc_contract_state_address_transaction_id_idx` (`address`, `transaction_id`, `height`),
    KEY `smc_contract_state_height_idx` (`height`, `address`, `transaction_id`)
) ENGINE = ROCKSDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;




CREATE TABLE `tag` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `tag` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `in_stock_count` int(11) NOT NULL,
  `total_count` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `tag_tag_idx` (`tag`,`height`),
  KEY `tag_in_stock_count_idx` (`in_stock_count`,`height`),
  KEY `tag_height_tag_idx` (`height`,`tag`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `tagged_data` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tags` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `parsed_tags` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`parsed_tags`)),
  `type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `data` blob NOT NULL,
  `is_text` tinyint(1) NOT NULL,
  `channel` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `filename` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `block_timestamp` int(11) NOT NULL,
  `transaction_timestamp` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `tagged_data_id_height_idx` (`id`,`height`),
  KEY `tagged_data_expiration_idx` (`transaction_timestamp`),
  KEY `tagged_data_account_id_height_idx` (`account_id`,`height`),
  KEY `tagged_data_block_timestamp_height_db_id_idx` (`block_timestamp`,`height`,`db_id`),
  KEY `tagged_data_channel_idx` (`channel`(191),`height`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `tagged_data_extend` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `extend_id` bigint(20) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  UNIQUE KEY `db_id` (`db_id`),
  KEY `tagged_data_extend_id_height_idx` (`id`,`height`),
  KEY `tagged_data_extend_height_id_idx` (`height`,`id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `tagged_data_timestamp` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `TIMESTAMP` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `tagged_data_timestamp_id_height_idx` (`id`,`height`),
  KEY `tagged_data_timestamp_height_id_idx` (`height`,`id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `trade` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `asset_id` bigint(20) NOT NULL,
  `block_id` bigint(20) NOT NULL,
  `ask_order_id` bigint(20) NOT NULL,
  `bid_order_id` bigint(20) NOT NULL,
  `ask_order_height` int(11) NOT NULL,
  `bid_order_height` int(11) NOT NULL,
  `seller_id` bigint(20) NOT NULL,
  `buyer_id` bigint(20) NOT NULL,
  `is_buy` tinyint(1) NOT NULL,
  `quantity` bigint(20) NOT NULL,
  `price` bigint(20) NOT NULL,
  `TIMESTAMP` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  UNIQUE KEY `db_id` (`db_id`),
  KEY `trade_asset_id_idx` (`asset_id`,`height`),
  KEY `trade_seller_id_idx` (`seller_id`,`height`),
  KEY `trade_buyer_id_idx` (`buyer_id`,`height`),
  KEY `trade_height_idx` (`height`),
  KEY `trade_ask_idx` (`ask_order_id`,`height`),
  KEY `trade_bid_idx` (`bid_order_id`,`height`),
  KEY `trade_height_db_id_idx` (`height`,`db_id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `transaction` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `deadline` smallint(6) NOT NULL,
  `recipient_id` bigint(20) DEFAULT NULL,
  `transaction_index` smallint(6) NOT NULL,
  `amount` bigint(20) NOT NULL,
  `fee` bigint(20) NOT NULL,
  `full_hash` binary(32) NOT NULL,
  `height` int(11) NOT NULL,
  `block_id` bigint(20) NOT NULL,
  `signature` blob DEFAULT NULL,
  `TIMESTAMP` int(11) NOT NULL,
  `type` tinyint(4) NOT NULL,
  `subtype` tinyint(4) NOT NULL,
  `sender_id` bigint(20) NOT NULL,
  `sender_public_key` binary(32) DEFAULT NULL,
  `block_timestamp` int(11) NOT NULL,
  `referenced_transaction_full_hash` binary(32) DEFAULT NULL,
  `phased` tinyint(1) NOT NULL DEFAULT 0,
  `attachment_bytes` blob DEFAULT NULL,
  `version` tinyint(4) NOT NULL,
  `has_message` tinyint(1) NOT NULL DEFAULT 0,
  `has_encrypted_message` tinyint(1) NOT NULL DEFAULT 0,
  `has_public_key_announcement` tinyint(1) NOT NULL DEFAULT 0,
  `ec_block_height` int(11) DEFAULT NULL,
  `ec_block_id` bigint(20) DEFAULT NULL,
  `has_encrypttoself_message` tinyint(1) NOT NULL DEFAULT 0,
  `has_prunable_message` tinyint(1) NOT NULL DEFAULT 0,
  `has_prunable_encrypted_message` tinyint(1) NOT NULL DEFAULT 0,
  `has_prunable_attachment` tinyint(1) NOT NULL DEFAULT 0,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `transaction_id_idx` (`id`),
  KEY `transaction_sender_id_idx` (`sender_id`),
  KEY `transaction_recipient_id_idx` (`recipient_id`),
  KEY `transaction_block_timestamp_idx` (`block_timestamp`),
  KEY `transaction_block_id_idx` (`block_id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `transaction_shard_index` (
  `transaction_id` bigint(20) NOT NULL,
  `partial_transaction_hash` blob NOT NULL,
  `transaction_index` smallint(6) NOT NULL,
  `height` int(11) NOT NULL,
  UNIQUE KEY `transaction_shard_index_height_transaction_index_idx` (`height`,`transaction_index`),
  UNIQUE KEY `transaction_shard_index_transaction_id_height_idx` (`transaction_id`,`height`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `trim` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `height` int(11) NOT NULL,
  `done` tinyint(1) NOT NULL DEFAULT 0,
  UNIQUE KEY `db_id` (`db_id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `two_factor_auth` (
  `account` bigint(20) NOT NULL,
  `secret` blob DEFAULT NULL,
  `confirmed` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`account`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `unconfirmed_transaction` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `expiration` int(11) NOT NULL,
  `transaction_height` int(11) NOT NULL,
  `fee_per_byte` bigint(20) NOT NULL,
  `arrival_timestamp` bigint(20) NOT NULL,
  `transaction_bytes` blob NOT NULL,
  `prunable_json` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `height` int(11) NOT NULL,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `unconfirmed_transaction_id_idx` (`id`),
  KEY `unconfirmed_transaction_height_fee_timestamp_idx` (`transaction_height`,`fee_per_byte`,`arrival_timestamp`),
  KEY `unconfirmed_transaction_expiration_idx` (`expiration`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `update_status` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `transaction_id` bigint(20) NOT NULL,
  `updated` tinyint(1) NOT NULL DEFAULT 0,
  UNIQUE KEY `db_id` (`db_id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `user_error_message` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `error` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `operation` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `details` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `timestamp` bigint(20) NOT NULL,
  UNIQUE KEY `db_id` (`db_id`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




CREATE TABLE `version` (
  `next_update` int(11) NOT NULL
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE `vote` (
  `db_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `poll_id` bigint(20) NOT NULL,
  `voter_id` bigint(20) NOT NULL,
  `vote_bytes` blob NOT NULL,
  `height` int(11) NOT NULL,
  UNIQUE KEY `db_id` (`db_id`),
  UNIQUE KEY `vote_id_idx` (`id`),
  UNIQUE KEY `vote_poll_id_idx` (`poll_id`,`voter_id`),
  KEY `vote_height_idx` (`height`)
) ENGINE=ROCKSDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;