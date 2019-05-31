/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import javax.sql.DataSource;

public class ShardInitTableSchemaVersion extends DbVersion {

    protected int update(int nextUpdate) {
        switch (nextUpdate) {
            case 1:
                apply("CREATE TABLE IF NOT EXISTS BLOCK (" +
                        "DB_ID BIGINT not null, " +
                        "ID BIGINT not null, " +
                        "VERSION INTEGER not null, " +
                        "TIMESTAMP INTEGER not null, " +
                        "PREVIOUS_BLOCK_ID BIGINT, " +
                        "TOTAL_AMOUNT BIGINT not null, " +
                        "TOTAL_FEE BIGINT not null, " +
                        "PAYLOAD_LENGTH INTEGER not null, " +
                        "PREVIOUS_BLOCK_HASH binary(32), " +
                        "CUMULATIVE_DIFFICULTY binary not null, " +
                        "BASE_TARGET BIGINT not null, " +
                        "NEXT_BLOCK_ID BIGINT, " +
                        "HEIGHT INTEGER not null, " +
                        "GENERATION_SIGNATURE binary(64) not null, " +
                        "BLOCK_SIGNATURE binary(64) not null, " +
                        "PAYLOAD_HASH binary(32) not null, " +
                        "GENERATOR_ID BIGINT not null, " +
                        "TIMEOUT INTEGER default 0 not null " +
                        ")");
            case 2:
                apply("CREATE TABLE IF NOT EXISTS TRANSACTION (" +
                        "db_id BIGINT not null, " +
                        "id BIGINT NOT NULL, " +
                        "deadline SMALLINT NOT NULL, " +
                        "recipient_id BIGINT, " +
                        "transaction_index SMALLINT NOT NULL, " +
                        "amount BIGINT NOT NULL, " +
                        "fee BIGINT NOT NULL, " +
                        "full_hash BINARY(32) NOT NULL, " +
                        "height INT NOT NULL, " +
                        "block_id BIGINT NOT NULL, " +
                        "signature BINARY(64) NOT NULL, " +
                        "timestamp INT NOT NULL, " +
                        "type TINYINT NOT NULL, " +
                        "subtype TINYINT NOT NULL, " +
                        "sender_id BIGINT NOT NULL, " +
                        "sender_public_key BINARY(32), "+
                        "block_timestamp INT NOT NULL, " +
                        "referenced_transaction_full_hash BINARY(32), " +
                        "phased BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "attachment_bytes VARBINARY, " +
                        "version TINYINT NOT NULL, " +
                        "has_message BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "has_encrypted_message BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "has_public_key_announcement BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "ec_block_height INT DEFAULT NULL, " +
                        "ec_block_id BIGINT DEFAULT NULL, " +
                        "has_encrypttoself_message BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "has_prunable_message BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "has_prunable_encrypted_message BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "has_prunable_attachment BOOLEAN NOT NULL DEFAULT FALSE)");
            case 3:
                apply("CREATE TABLE IF NOT EXISTS ALIAS (" +
                        "DB_ID BIGINT NOT NULL, " +
                        "ID BIGINT NOT NULL, " +
                        "ACCOUNT_ID BIGINT NOT NULL, " +
                        "ALIAS_NAME VARCHAR NOT NULL, " +
                        "ALIAS_NAME_LOWER VARCHAR DEFAULT 'LOWER(ALIAS_NAME)' NOT NULL," +
                        "ALIAS_URI VARCHAR NOT NULL, " +
                        "timestamp INTEGER NOT NULL, " +
                        "HEIGHT INTEGER NOT NULL, " +
                        "LATEST BOOLEAN DEFAULT TRUE NOT NULL )");
            case 4:
                apply("CREATE TABLE IF NOT EXISTS ALIAS_OFFER (" +
                        "DB_ID BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "price BIGINT NOT NULL, " +
                        "buyer_id BIGINT, " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN DEFAULT TRUE NOT NULL )");
            case 5:
                apply("CREATE TABLE IF NOT EXISTS ASSET (" +
                        "DB_ID BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "account_id BIGINT NOT NULL, " +
                        "name VARCHAR NOT NULL, " +
                        "description VARCHAR, " +
                        "quantity BIGINT NOT NULL, " +
                        "decimals TINYINT NOT NULL, " +
                        "initial_quantity BIGINT NOT NULL, " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 6:
                apply("CREATE TABLE IF NOT EXISTS TRADE (" +
                        "DB_ID BIGINT NOT NULL, " +
                        "asset_id BIGINT NOT NULL, " +
                        "block_id BIGINT NOT NULL, " +
                        "ask_order_id BIGINT NOT NULL, " +
                        "bid_order_id BIGINT NOT NULL, " +
                        "ask_order_height INT NOT NULL, " +
                        "bid_order_height INT NOT NULL, " +
                        "seller_id BIGINT NOT NULL, " +
                        "buyer_id BIGINT NOT NULL, " +
                        "is_buy BOOLEAN NOT NULL, " +
                        "quantity BIGINT NOT NULL, " +
                        "price BIGINT NOT NULL, " +
                        "timestamp INT NOT NULL, " +
                        "height INT NOT NULL)");
            case 7:
                apply("CREATE TABLE IF NOT EXISTS ASK_ORDER (" +
                        "db_id BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "account_id BIGINT NOT NULL, " +
                        "asset_id BIGINT NOT NULL, " +
                        "price BIGINT NOT NULL, " +
                        "transaction_index SMALLINT NOT NULL, " +
                        "transaction_height INT NOT NULL, " +
                        "quantity BIGINT NOT NULL, " +
                        "creation_height INT NOT NULL, " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 8:
                apply("CREATE TABLE IF NOT EXISTS BID_ORDER (" +
                        "db_id BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "account_id BIGINT NOT NULL, " +
                        "asset_id BIGINT NOT NULL, " +
                        "price BIGINT NOT NULL, " +
                        "transaction_index SMALLINT NOT NULL, " +
                        "transaction_height INT NOT NULL, " +
                        "quantity BIGINT NOT NULL, " +
                        "creation_height INT NOT NULL, " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 9:
                apply("CREATE TABLE IF NOT EXISTS GOODS (" +
                        "db_id BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "seller_id BIGINT NOT NULL, " +
                        "name VARCHAR NOT NULL, " +
                        "description VARCHAR, " +
                        "parsed_tags ARRAY, " +
                        "has_image BOOLEAN NOT NULL, " +
                        "tags VARCHAR, " +
                        "timestamp INT NOT NULL, " +
                        "quantity INT NOT NULL, " +
                        "price BIGINT NOT NULL, " +
                        "delisted BOOLEAN NOT NULL, " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");

            case 10:
                apply("CREATE TABLE IF NOT EXISTS PURCHASE (" +
                        "db_id BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "buyer_id BIGINT NOT NULL, " +
                        "goods_id BIGINT NOT NULL, " +
                        "seller_id BIGINT NOT NULL, " +
                        "quantity INT NOT NULL, " +
                        "price BIGINT NOT NULL, " +
                        "deadline INT NOT NULL, " +
                        "note VARBINARY, " +
                        "nonce BINARY(32), " +
                        "timestamp INT NOT NULL, " +
                        "pending BOOLEAN NOT NULL, " +
                        "goods VARBINARY, " +
                        "goods_nonce BINARY(32), " +
                        "goods_is_text BOOLEAN NOT NULL DEFAULT TRUE, " +
                        "refund_note VARBINARY, " +
                        "refund_nonce BINARY(32), " +
                        "has_feedback_notes BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "has_public_feedbacks BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "discount BIGINT NOT NULL, " +
                        "refund BIGINT NOT NULL, " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 11:
                apply("CREATE TABLE IF NOT EXISTS ACCOUNT (" +
                        "db_id BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "balance BIGINT NOT NULL, " +
                        "unconfirmed_balance BIGINT NOT NULL, " +
                        "has_control_phasing BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "forged_balance BIGINT NOT NULL, " +
                        "active_lessee_id BIGINT, " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 12:
                apply("CREATE TABLE IF NOT EXISTS ACCOUNT_ASSET (" +
                        "db_id BIGINT NOT NULL, " +
                        "account_id BIGINT NOT NULL, " +
                        "asset_id BIGINT NOT NULL, " +
                        "quantity BIGINT NOT NULL, " +
                        "unconfirmed_quantity BIGINT NOT NULL, " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 13:
                apply("CREATE TABLE IF NOT EXISTS ACCOUNT_GUARANTEED_BALANCE (" +
                        "db_id BIGINT NOT NULL, " +
                        "account_id BIGINT NOT NULL, " +
                        "additions BIGINT NOT NULL, " +
                        "height INT NOT NULL)");
            case 14:
                apply("CREATE TABLE IF NOT EXISTS PURCHASE_FEEDBACK (" +
                        "db_id BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "feedback_data VARBINARY NOT NULL, " +
                        "feedback_nonce BINARY(32) NOT NULL, " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 15:
                apply("CREATE TABLE IF NOT EXISTS PURCHASE_PUBLIC_FEEDBACK (" +
                        "db_id BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "public_feedback VARCHAR NOT NULL, " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 16:
                apply("CREATE TABLE IF NOT EXISTS UNCONFIRMED_TRANSACTION (" +
                        "db_id BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "expiration INT NOT NULL, "+
                        "transaction_height INT NOT NULL, " +
                        "fee_per_byte BIGINT NOT NULL, " +
                        "arrival_timestamp BIGINT NOT NULL, " +
                        "transaction_bytes VARBINARY NOT NULL, " +
                        "prunable_json VARCHAR, " +
                        "height INT NOT NULL)");
            case 17:
                apply("CREATE TABLE IF NOT EXISTS ASSET_TRANSFER (" +
                        "db_id BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "asset_id BIGINT NOT NULL, " +
                        "sender_id BIGINT NOT NULL, " +
                        "recipient_id BIGINT NOT NULL, " +
                        "quantity BIGINT NOT NULL, " +
                        "timestamp INT NOT NULL, " +
                        "height INT NOT NULL)");
            case 18:
                apply("CREATE TABLE IF NOT EXISTS TAG (" +
                        "db_id BIGINT NOT NULL, " +
                        "tag VARCHAR NOT NULL, " +
                        "in_stock_count INT NOT NULL, " +
                        "total_count INT NOT NULL, " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 19:
                apply("CREATE TABLE IF NOT EXISTS CURRENCY (" +
                        "db_id BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "account_id BIGINT NOT NULL, " +
                        "name VARCHAR NOT NULL, " +
                        "name_lower VARCHAR AS LOWER (name) NOT NULL, " +
                        "code VARCHAR NOT NULL, " +
                        "description VARCHAR, " +
                        "type INT NOT NULL, " +
                        "initial_supply BIGINT NOT NULL DEFAULT 0, " +
                        "reserve_supply BIGINT NOT NULL, " +
                        "max_supply BIGINT NOT NULL, " +
                        "creation_height INT NOT NULL, " +
                        "issuance_height INT NOT NULL, " +
                        "min_reserve_per_unit_atm BIGINT NOT NULL, " +
                        "min_difficulty TINYINT NOT NULL, " +
                        "max_difficulty TINYINT NOT NULL, " +
                        "ruleset TINYINT NOT NULL, " +
                        "algorithm TINYINT NOT NULL, " +
                        "decimals TINYINT NOT NULL DEFAULT 0," +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 20:
                apply("CREATE TABLE IF NOT EXISTS ACCOUNT_CURRENCY (" +
                        "db_id BIGINT NOT NULL, " +
                        "account_id BIGINT NOT NULL, " +
                        "currency_id BIGINT NOT NULL, " +
                        "units BIGINT NOT NULL, " +
                        "unconfirmed_units BIGINT NOT NULL, " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 21:
                apply("CREATE TABLE IF NOT EXISTS CURRENCY_FOUNDER (" +
                        "db_id BIGINT NOT NULL, " +
                        "currency_id BIGINT NOT NULL, " +
                        "account_id BIGINT NOT NULL, " +
                        "amount BIGINT NOT NULL, " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 22:
                apply("CREATE TABLE IF NOT EXISTS CURRENCY_MINT (" +
                        "db_id BIGINT NOT NULL, " +
                        "currency_id BIGINT NOT NULL, " +
                        "account_id BIGINT NOT NULL, " +
                        "counter BIGINT NOT NULL, " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 23:
                apply("CREATE TABLE IF NOT EXISTS BUY_OFFER (" +
                        "db_id BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "currency_id BIGINT NOT NULL, " +
                        "account_id BIGINT NOT NULL," +
                        "rate BIGINT NOT NULL, " +
                        "unit_limit BIGINT NOT NULL, " +
                        "supply BIGINT NOT NULL, " +
                        "expiration_height INT NOT NULL," +
                        "creation_height INT NOT NULL, " +
                        "transaction_index SMALLINT NOT NULL, " +
                        "transaction_height INT NOT NULL, " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 24:
                apply("CREATE TABLE IF NOT EXISTS SELL_OFFER (" +
                        "db_id BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "currency_id BIGINT NOT NULL, " +
                        "account_id BIGINT NOT NULL, " +
                        "rate BIGINT NOT NULL, " +
                        "unit_limit BIGINT NOT NULL, " +
                        "supply BIGINT NOT NULL, " +
                        "expiration_height INT NOT NULL, " +
                        "creation_height INT NOT NULL, " +
                        "transaction_index SMALLINT NOT NULL, " +
                        "transaction_height INT NOT NULL, " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 25:
                apply("CREATE TABLE IF NOT EXISTS EXCHANGE (" +
                        "db_id BIGINT NOT NULL, " +
                        "transaction_id BIGINT NOT NULL, " +
                        "currency_id BIGINT NOT NULL, " +
                        "block_id BIGINT NOT NULL, " +
                        "offer_id BIGINT NOT NULL, " +
                        "seller_id BIGINT NOT NULL, " +
                        "buyer_id BIGINT NOT NULL, " +
                        "units BIGINT NOT NULL, " +
                        "rate BIGINT NOT NULL, " +
                        "timestamp INT NOT NULL, " +
                        "height INT NOT NULL)");
            case 26:
                apply("CREATE TABLE IF NOT EXISTS CURRENCY_TRANSFER (" +
                        "db_id BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "currency_id BIGINT NOT NULL, " +
                        "sender_id BIGINT NOT NULL, " +
                        "recipient_id BIGINT NOT NULL, " +
                        "units BIGINT NOT NULL, " +
                        "timestamp INT NOT NULL, " +
                        "height INT NOT NULL)");
            case 27:
                apply("CREATE TABLE IF NOT EXISTS SCAN (" +
                        "rescan BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "height INT NOT NULL DEFAULT 0, " +
                        "validate BOOLEAN NOT NULL DEFAULT FALSE)");
            case 28:
                apply("CREATE TABLE IF NOT EXISTS CURRENCY_SUPPLY (" +
                        "db_id BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "current_supply BIGINT NOT NULL, " +
                        "current_reserve_per_unit_atm BIGINT NOT NULL, " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 29:
                apply("CREATE TABLE IF NOT EXISTS PUBLIC_KEY (" +
                        "db_id BIGINT NOT NULL, " +
                        "account_id BIGINT NOT NULL, " +
                        "public_key BINARY(32), " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 30:
                apply("CREATE TABLE IF NOT EXISTS VOTE (" +
                        "db_id BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "poll_id BIGINT NOT NULL, " +
                        "voter_id BIGINT NOT NULL, " +
                        "vote_bytes VARBINARY NOT NULL, " +
                        "height INT NOT NULL)");
            case 31:
                apply("CREATE TABLE IF NOT EXISTS POLL (" +
                        "db_id BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "account_id BIGINT NOT NULL, " +
                        "name VARCHAR NOT NULL, " +
                        "description VARCHAR, " +
                        "options ARRAY NOT NULL, " +
                        "min_num_options TINYINT, " +
                        "max_num_options TINYINT, " +
                        "min_range_value TINYINT, " +
                        "max_range_value TINYINT, " +
                        "timestamp INT NOT NULL, " +
                        "finish_height INT NOT NULL, " +
                        "voting_model TINYINT NOT NULL, " +
                        "min_balance BIGINT, " +
                        "min_balance_model TINYINT, " +
                        "holding_id BIGINT, " +
                        "height INT NOT NULL)");
            case 32:
                apply("CREATE TABLE IF NOT EXISTS POLL_RESULT (" +
                        "db_id BIGINT NOT NULL, " +
                        "poll_id BIGINT NOT NULL, " +
                        "result BIGINT, " +
                        "weight BIGINT NOT NULL, " +
                        "height INT NOT NULL)");
            case 33:
                apply("CREATE TABLE IF NOT EXISTS PHASING_POLL (" +
                        "db_id BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "account_id BIGINT NOT NULL, " +
                        "whitelist_size TINYINT NOT NULL DEFAULT 0, " +
                        "finish_height INT NOT NULL, " +
                        "voting_model TINYINT NOT NULL, " +
                        "quorum BIGINT, " +
                        "min_balance BIGINT, " +
                        "holding_id BIGINT, " +
                        "min_balance_model TINYINT, " +
                        "hashed_secret VARBINARY, " +
                        "algorithm TINYINT, " +
                        "height INT NOT NULL, " +
                        "finish_time INT NOT NULL DEFAULT -1)");
            case 34:
                apply("CREATE TABLE IF NOT EXISTS PHASING_VOTE (" +
                        "db_id BIGINT NOT NULL, " +
                        "vote_id BIGINT NOT NULL, " +
                        "transaction_id BIGINT NOT NULL, " +
                        "voter_id BIGINT NOT NULL, " +
                        "height INT NOT NULL)");
            case 35:
                apply("CREATE TABLE IF NOT EXISTS PHASING_POLL_VOTER (" +
                        "db_id BIGINT NOT NULL, " +
                        "transaction_id BIGINT NOT NULL, " +
                        "voter_id BIGINT NOT NULL, " +
                        "height INT NOT NULL)");
            case 36:
                apply("CREATE TABLE IF NOT EXISTS PHASING_POLL_RESULT (" +
                        "db_id BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "result BIGINT NOT NULL, " +
                        "approved BOOLEAN NOT NULL, " +
                        "height INT NOT NULL)");
            case 37:
                apply("CREATE TABLE IF NOT EXISTS ACCOUNT_INFO (" +
                        "db_id BIGINT NOT NULL, " +
                        "account_id BIGINT NOT NULL, " +
                        "name VARCHAR, " +
                        "description VARCHAR, " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 38:
                apply("CREATE TABLE IF NOT EXISTS PRUNABLE_MESSAGE (" +
                        "db_id BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "sender_id BIGINT NOT NULL, " +
                        "recipient_id BIGINT, " +
                        "message VARBINARY, " +
                        "message_is_text BOOLEAN NOT NULL, " +
                        "is_compressed BOOLEAN NOT NULL, " +
                        "encrypted_message VARBINARY, " +
                        "encrypted_is_text BOOLEAN DEFAULT FALSE, " +
                        "block_timestamp INT NOT NULL, " +
                        "transaction_timestamp INT NOT NULL, " +
                        "height INT NOT NULL)");
            case 39:
                apply("CREATE TABLE IF NOT EXISTS TAGGED_DATA (" +
                        "db_id BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "account_id BIGINT NOT NULL, " +
                        "name VARCHAR NOT NULL, " +
                        "description VARCHAR, " +
                        "tags VARCHAR, " +
                        "parsed_tags ARRAY, " +
                        "type VARCHAR, " +
                        "data VARBINARY NOT NULL, " +
                        "is_text BOOLEAN NOT NULL, " +
                        "channel VARCHAR, " +
                        "filename VARCHAR, " +
                        "block_timestamp INT NOT NULL, " +
                        "transaction_timestamp INT NOT NULL, " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 40:
                apply("CREATE TABLE IF NOT EXISTS DATA_TAG (" +
                        "db_id BIGINT NOT NULL, " +
                        "tag VARCHAR NOT NULL, " +
                        "tag_count INT NOT NULL, " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 41:
                apply("CREATE TABLE IF NOT EXISTS TAGGED_DATA_TIMESTAMP (" +
                        "db_id BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "timestamp INT NOT NULL, " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 42:
                apply("CREATE TABLE IF NOT EXISTS ACCOUNT_LEASE (" +
                        "db_id BIGINT NOT NULL, " +
                        "lessor_id BIGINT NOT NULL, " +
                        "current_leasing_height_from INT, " +
                        "current_leasing_height_to INT, " +
                        "current_lessee_id BIGINT, " +
                        "next_leasing_height_from INT, " +
                        "next_leasing_height_to INT, " +
                        "next_lessee_id BIGINT, " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 43:
                apply("CREATE TABLE IF NOT EXISTS EXCHANGE_REQUEST (" +
                        "db_id BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "account_id BIGINT NOT NULL, " +
                        "currency_id BIGINT NOT NULL, " +
                        "units BIGINT NOT NULL, " +
                        "rate BIGINT NOT NULL, " +
                        "is_buy BOOLEAN NOT NULL, " +
                        "timestamp INT NOT NULL, " +
                        "height INT NOT NULL)");
            case 44:
                apply("CREATE TABLE IF NOT EXISTS ACCOUNT_LEDGER (" +
                        "db_id BIGINT NOT NULL, " +
                        "account_id BIGINT NOT NULL, " +
                        "event_type TINYINT NOT NULL, " +
                        "event_id BIGINT NOT NULL, " +
                        "holding_type TINYINT NOT NULL, " +
                        "holding_id BIGINT, " +
                        "change BIGINT NOT NULL, " +
                        "balance BIGINT NOT NULL, " +
                        "block_id BIGINT NOT NULL, " +
                        "height INT NOT NULL, " +
                        "timestamp INT NOT NULL)");
            case 45:
                apply("CREATE TABLE IF NOT EXISTS TAGGED_DATA_EXTEND (" +
                        "db_id BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "extend_id BIGINT NOT NULL, " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 46:
                apply("CREATE TABLE IF NOT EXISTS SHUFFLING (" +
                        "db_id BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "holding_id BIGINT NULL, " +
                        "holding_type TINYINT NOT NULL, " +
                        "issuer_id BIGINT NOT NULL, " +
                        "amount BIGINT NOT NULL, " +
                        "participant_count TINYINT NOT NULL, " +
                        "blocks_remaining SMALLINT NULL, " +
                        "stage TINYINT NOT NULL, " +
                        "assignee_account_id BIGINT NULL, " +
                        "registrant_count TINYINT NOT NULL, " +
                        "recipient_public_keys ARRAY, " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 47:
                apply("CREATE TABLE IF NOT EXISTS SHUFFLING_PARTICIPANT (" +
                        "db_id BIGINT NOT NULL, " +
                        "shuffling_id BIGINT NOT NULL, " +
                        "account_id BIGINT NOT NULL, " +
                        "next_account_id BIGINT NULL, " +
                        "participant_index TINYINT NOT NULL, " +
                        "state TINYINT NOT NULL, " +
                        "blame_data ARRAY, " +
                        "key_seeds ARRAY, " +
                        "data_transaction_full_hash BINARY(32), " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 48:
                apply("CREATE TABLE IF NOT EXISTS SHUFFLING_DATA (" +
                        "db_id BIGINT NOT NULL, " +
                        "shuffling_id BIGINT NOT NULL, " +
                        "account_id BIGINT NOT NULL, " +
                        "data ARRAY, " +
                        "transaction_timestamp INT NOT NULL, " +
                        "height INT NOT NULL)");
            case 49:
                apply("CREATE TABLE IF NOT EXISTS PHASING_POLL_LINKED_TRANSACTION (" +
                        "db_id BIGINT NOT NULL, " +
                        "transaction_id BIGINT NOT NULL, " +
                        "linked_full_hash BINARY(32) NOT NULL, " +
                        "linked_transaction_id BIGINT NOT NULL, " +
                        "height INT NOT NULL)");
            case 50:
                apply("CREATE TABLE IF NOT EXISTS PHASING_POLL_LINKED_TRANSACTION (" +
                        "db_id BIGINT NOT NULL, " +
                        "transaction_id BIGINT NOT NULL, " +
                        "linked_full_hash BINARY(32) NOT NULL, " +
                        "linked_transaction_id BIGINT NOT NULL, " +
                        "height INT NOT NULL)");
            case 51:
                apply("CREATE TABLE IF NOT EXISTS ACCOUNT_CONTROL_PHASING (" +
                        "db_id BIGINT NOT NULL, " +
                        "account_id BIGINT NOT NULL, " +
                        "whitelist ARRAY, " +
                        "voting_model TINYINT NOT NULL, " +
                        "quorum BIGINT, " +
                        "min_balance BIGINT, " +
                        "holding_id BIGINT, " +
                        "min_balance_model TINYINT, " +
                        "max_fees BIGINT, " +
                        "min_duration SMALLINT, " +
                        "max_duration SMALLINT, " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 52:
                apply("CREATE TABLE IF NOT EXISTS ACCOUNT_PROPERTY (" +
                        "db_id BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "recipient_id BIGINT NOT NULL, " +
                        "setter_id BIGINT, " +
                        "property VARCHAR NOT NULL, " +
                        "value VARCHAR, " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 53:
                apply("CREATE TABLE IF NOT EXISTS ASSET_DELETE (" +
                        "db_id BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "asset_id BIGINT NOT NULL, " +
                        "account_id BIGINT NOT NULL, " +
                        "quantity BIGINT NOT NULL, " +
                        "timestamp INT NOT NULL, " +
                        "height INT NOT NULL)");
            case 54:
                apply("CREATE TABLE IF NOT EXISTS REFERENCED_TRANSACTION (" +
                        "db_id BIGINT NOT NULL, " +
                        "transaction_id BIGINT NOT NULL, " +
                        "referenced_transaction_id BIGINT NOT NULL, " +
                        "height INT NOT NULL DEFAULT -1)");
            case 55:
                apply("CREATE TABLE IF NOT EXISTS ASSET_DIVIDEND (" +
                        "db_id BIGINT NOT NULL, " +
                        "id BIGINT NOT NULL, " +
                        "asset_id BIGINT NOT NULL, " +
                        "amount BIGINT NOT NULL, " +
                        "dividend_height INT NOT NULL, " +
                        "total_dividend BIGINT NOT NULL, " +
                        "num_accounts BIGINT NOT NULL, " +
                        "timestamp INT NOT NULL, " +
                        "height INT NOT NULL)");
            case 56:
                apply("CREATE TABLE IF NOT EXISTS UPDATE_STATUS (" +
                        "db_id BIGINT NOT NULL, " +
                        "transaction_id BIGINT NOT NULL, " +
                        "updated BOOLEAN NOT NULL DEFAULT FALSE )");
            case 57:
                apply("CREATE TABLE IF NOT EXISTS GENESIS_PUBLIC_KEY (" +
                        "db_id BIGINT NOT NULL," +
                        "account_id BIGINT NOT NULL, " +
                        "public_key BINARY(32), " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 58:
                apply("CREATE TABLE IF NOT EXISTS TWO_FACTOR_AUTH (" +
                        "account BIGINT PRIMARY KEY," + // TODO: replace
                        "secret VARBINARY," +
                        "confirmed BOOLEAN NOT NULL DEFAULT FALSE )"
                );
            case 60:
                apply("CREATE TABLE IF NOT EXISTS OPTION (" +
                        "name VARCHAR(100) not null, " +
                        "value VARCHAR(250))");
            case 61:
                apply("CREATE TABLE IF NOT EXISTS SHARD (" +
                        "shard_id BIGINT AUTO_INCREMENT NOT NULL, " +
                        "shard_hash VARBINARY, " +
                        "shard_height INT not null default 0, " +
                        "shard_state BIGINT default 0, " +
                        "zip_hash_crc VARBINARY)");
            case 62:
                apply("CREATE TABLE IF NOT EXISTS BLOCK_INDEX (" +
                        "shard_id BIGINT NOT NULL, " +
                        "block_id BIGINT NOT NULL, " +
                        "block_height INT NOT NULL)");
            case 63:
                apply("CREATE TABLE IF NOT EXISTS TRANSACTION_SHARD_INDEX (" +
                        "transaction_id BIGINT NOT NULL, " +
                        "partial_transaction_hash VARBINARY NOT NULL, " +
                        "block_id BIGINT NOT NULL, " +
                        "zip_hash_crc VARBINARY )");
            case 64:
                apply("CREATE TABLE IF NOT EXISTS SHARD_RECOVERY (" +
                        "shard_recovery_id BIGINT AUTO_INCREMENT NOT NULL, " +
                        "state VARCHAR NOT NULL, " +
                        "object_name VARCHAR NULL, " +
                        "column_name VARCHAR NULL, " +
                        "last_column_value BIGINT, " +
                        "processed_object VARCHAR, " +
                        "updated TIMESTAMP(9) NOT NULL)");
            case 65:
                apply("CREATE TABLE IF NOT EXISTS DEX_OFFER (" +
                        "db_id BIGINT NOT NULL, " +
                        "transaction_id BIGINT NOT NULL, " +
                        "type TINYINT NOT NULL, " +
                        "account_id BIGINT NOT NULL, " +
                        "offer_currency TINYINT NOT NULL, " +
                        "offer_amount BIGINT NOT NULL, " +
                        "pair_currency TINYINT NOT NULL, " +
                        "pair_rate DECIMAL NOT NULL, " +
                        "finish_time INT NOT NULL, " +
                        "status TINYINT NOT NULL, " +
                        "height INT NOT NULL, " +
                        "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 66:
                return 66;
            case 334:
                return 334;
            default:
                throw new RuntimeException("Shard CREATE TABLES database is inconsistent with code, at update " + nextUpdate
                        + ", probably trying to run older code on newer database");
        }
    }

    @Override
    void init(DataSource dataSource) {
        super.init(dataSource);
    }

    @Override
    protected void apply(String sql) {
        super.apply(sql);
    }

    @Override
    public String toString() {
        return "ShardInitTableSchemaVersion";
    }
}
