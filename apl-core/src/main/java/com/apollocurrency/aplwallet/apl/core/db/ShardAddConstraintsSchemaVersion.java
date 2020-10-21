/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import org.slf4j.Logger;

import javax.sql.DataSource;

import static org.slf4j.LoggerFactory.getLogger;

public class ShardAddConstraintsSchemaVersion extends ShardInitTableSchemaVersion {
    private static final Logger log = getLogger(ShardAddConstraintsSchemaVersion.class);
    private static final int startNumber = 5;

    protected int update(int nextUpdate) {
        if (nextUpdate < startNumber) {
            nextUpdate = super.update(nextUpdate);
        }
        switch (nextUpdate) {
            /*  ---------------------- BLOCK -------------------    */
            case startNumber:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("ALTER TABLE block ADD CONSTRAINT chk_timeout CHECK (timeout >= 0)");
            case startNumber + 1:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("ALTER TABLE block ADD CONSTRAINT primary_key_block_id PRIMARY KEY (id)"); // PK + unique index
            case startNumber + 2:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("ALTER TABLE block ADD CONSTRAINT block_timestamp_idx unique (`TIMESTAMP` DESC)");
            case startNumber + 3:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("ALTER TABLE block ADD CONSTRAINT block_height_idx unique (height)");
            case startNumber + 4:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("CREATE UNIQUE INDEX IF NOT EXISTS primary_key_block_db_id_index on block (db_id)");
            case startNumber + 5:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("CREATE INDEX IF NOT EXISTS block_generator_id_idx on block (generator_id)");
            case startNumber + 6:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("CREATE UNIQUE INDEX IF NOT EXISTS block_id_idx ON block (id)");
            case startNumber + 7:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("CREATE UNIQUE INDEX IF NOT EXISTS block_height_idx ON block (height)");
            case startNumber + 8:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
//                apply("CREATE INDEX IF NOT EXISTS block_generator_id_idx ON block (generator_id)");
                apply(null);
            case startNumber + 9:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("CREATE UNIQUE INDEX IF NOT EXISTS block_timestamp_idx ON block (timestamp DESC)");
                /*  ---------------------- TRANSACTION -------------------    */
            case startNumber + 10:
                apply(null); //left for compatibility with existing dbs
            case startNumber + 11:
                log.trace("Starting adding TRANSACTION constraint = {}", nextUpdate);
                apply("ALTER TABLE transaction ADD CONSTRAINT primary_key_transaction_db_id PRIMARY KEY (DB_ID)"); // PK + unique index
            case startNumber + 12:
                log.trace("Starting adding TRANSACTION constraint = {}", nextUpdate);
                apply("CREATE UNIQUE INDEX IF NOT EXISTS transaction_id_idx ON transaction (id)");
            case startNumber + 13:
                log.trace("Starting adding TRANSACTION constraint = {}", nextUpdate);
                apply("CREATE INDEX IF NOT EXISTS transaction_sender_id_idx ON transaction (sender_id)");
            case startNumber + 14:
                log.trace("Starting adding TRANSACTION constraint = {}", nextUpdate);
                apply("CREATE INDEX IF NOT EXISTS transaction_recipient_id_idx ON transaction (recipient_id)");
            case startNumber + 15:
                log.trace("Starting adding TRANSACTION constraint = {}", nextUpdate);
                apply("CREATE INDEX IF NOT EXISTS transaction_block_timestamp_idx ON transaction (block_timestamp DESC)");
            case startNumber + 16:
                apply("ALTER TABLE transaction DROP CONSTRAINT IF EXISTS transaction_id_idx");
            case startNumber + 17:
                apply("CREATE UNIQUE INDEX transaction_block_id_transaction_index_idx ON transaction (block_id, transaction_index)");
            case startNumber + 18:
//                apply("ANALYZE");
                apply(null);
            case startNumber + 19:
                return startNumber + 19;
            default:
                throw new RuntimeException("Shard ADD CONSTRAINTS/INDEXES database is inconsistent with code, at update " + nextUpdate
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
        return "ShardAddConstraintsSchemaVersion";
    }
}
