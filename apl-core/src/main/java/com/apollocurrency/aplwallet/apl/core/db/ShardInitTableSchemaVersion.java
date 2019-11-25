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
                        "\"timestamp\" INTEGER not null, " +
                        "PREVIOUS_BLOCK_ID BIGINT, " +
                        "TOTAL_AMOUNT BIGINT not null, " +
                        "TOTAL_FEE BIGINT not null, " +
                        "PAYLOAD_LENGTH INTEGER not null, " +
                        "PREVIOUS_BLOCK_HASH BYTEA, " +
                        "CUMULATIVE_DIFFICULTY BYTEA not null, " +
                        "BASE_TARGET BIGINT not null, " +
                        "NEXT_BLOCK_ID BIGINT, " +
                        "HEIGHT INTEGER not null, " +
                        "GENERATION_SIGNATURE BYTEA not null, " +
                        "BLOCK_SIGNATURE BYTEA not null, " +
                        "PAYLOAD_HASH BYTEA not null, " +
                        "GENERATOR_ID BIGINT not null, " +
                        "TIMEOUT INTEGER default 0 not null" +
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
                        "full_hash BYTEA NOT NULL, " +
                        "height INT NOT NULL, " +
                        "block_id BIGINT NOT NULL, " +
//                        "FOREIGN KEY (block_id) REFERENCES block (id) ON DELETE CASCADE, "
                        "signature BYTEA NOT NULL, " +
                        "timestamp INT NOT NULL, " +
                        "type SMALLINT NOT NULL, " +
                        "subtype SMALLINT NOT NULL, " +
                        "sender_id BIGINT NOT NULL, " +
                        "sender_public_key BYTEA, "+
                        "block_timestamp INT NOT NULL, " +
                        "referenced_transaction_full_hash BYTEA, " +
                        "phased BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "attachment_bytes BYTEA, " +
                        "version SMALLINT NOT NULL, " +
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
                apply("CREATE TABLE IF NOT EXISTS option (name VARCHAR(100) not null, value VARCHAR(250))");
            case 4:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS option_name_value_idx ON option(name, value)");
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
