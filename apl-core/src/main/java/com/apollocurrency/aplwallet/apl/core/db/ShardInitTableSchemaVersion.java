/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import javax.sql.DataSource;

public class ShardInitTableSchemaVersion extends DbVersion {

    protected int update(int nextUpdate) {
        switch (nextUpdate) {
            case 1:
                apply("CREATE TABLE IF NOT EXISTS block (" +
                    "db_id BIGINT UNSIGNED NOT NULL, " +
                    "id BIGINT NOT NULL, " +
                    "version INT NOT NULL, " +
                    "`TIMESTAMP` INT NOT NULL, " +
                    "previous_block_id BIGINT, " +
                    "total_amount BIGINT NOT NULL, " +
                    "total_fee BIGINT NOT NULL, " +
                    "payload_length INT NOT NULL, " +
                    "previous_block_hash BINARY(32), " +
                    "cumulative_difficulty BLOB NOT NULL, " +
                    "base_target BIGINT NOT NULL, " +
                    "next_block_id BIGINT, " +
                    "height INT NOT NULL, " +
                    "generation_signature BINARY(32) NOT NULL, " +
                    "block_signature BINARY(64), " +
                    "payload_hash BINARY(32) NOT NULL, " +
                    "generator_id BIGINT NOT NULL, " +
                    "timeout INT NOT NULL DEFAULT 0" +
                    ") ENGINE=ROCKSDB;");
            case 2:
                apply("CREATE TABLE IF NOT EXISTS transaction (" +
                    "db_id BIGINT UNSIGNED NOT NULL, " +
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
                    "`TIMESTAMP` INT NOT NULL, " +
                    "type TINYINT NOT NULL, " +
                    "subtype TINYINT NOT NULL, " +
                    "sender_id BIGINT NOT NULL, " +
                    "sender_public_key BINARY(32), " +
                    "block_timestamp INT NOT NULL, " +
                    "referenced_transaction_full_hash BINARY(32), " +
                    "phased BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "attachment_bytes BLOB, " +
                    "version TINYINT NOT NULL, " +
                    "has_message BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "has_encrypted_message BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "has_public_key_announcement BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "ec_block_height INT DEFAULT NULL, " +
                    "ec_block_id BIGINT DEFAULT NULL, " +
                    "has_encrypttoself_message BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "has_prunable_message BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "has_prunable_encrypted_message BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "has_prunable_attachment BOOLEAN NOT NULL DEFAULT FALSE) ENGINE=ROCKSDB;");

            case 3:
                apply("CREATE TABLE IF NOT EXISTS option (name VARCHAR(100) not null, `VALUE` VARCHAR(150)) ENGINE=ROCKSDB;");
            case 4:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS option_name_value_idx ON option(name, `VALUE`)");
            case 5:
                return 5;
            case 24:
                return 24;
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

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj.getClass() == this.getClass();
    }
}
