/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.slf4j.LoggerFactory.getLogger;

import javax.sql.DataSource;

import org.slf4j.Logger;

public class ShardAddConstraintsSchemaVersion extends ShardInitTableSchemaVersion {
    private static final Logger log = getLogger(ShardAddConstraintsSchemaVersion.class);
    private static final int startNumber = 6;

    protected int update(int nextUpdate) {
        nextUpdate = super.update(nextUpdate);
        switch (nextUpdate) {
/*  ---------------------- BLOCK -------------------    */
            case startNumber:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("ALTER TABLE block ADD CONSTRAINT IF NOT EXISTS chk_timeout CHECK (timeout >= 0)");
            case startNumber + 1:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("alter table BLOCK add constraint IF NOT EXISTS PRIMARY_KEY_BLOCK_ID primary key (ID)"); // PK + unique index
            case startNumber + 2:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("alter table BLOCK add constraint IF NOT EXISTS BLOCK_TIMESTAMP_IDX unique (TIMESTAMP)");
            case startNumber + 3:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("alter table BLOCK add constraint IF NOT EXISTS BLOCK_HEIGHT_IDX unique (HEIGHT)");
            case startNumber + 4:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("create unique index IF NOT EXISTS PRIMARY_KEY_BLOCK_DB_ID_INDEX on BLOCK (DB_ID)");
            case startNumber + 5:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("create index IF NOT EXISTS BLOCK_GENERATOR_ID_IDX on BLOCK (GENERATOR_ID)");
            case startNumber + 6:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("CREATE UNIQUE INDEX IF NOT EXISTS block_id_idx ON block (id)");
            case startNumber + 7:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("CREATE UNIQUE INDEX IF NOT EXISTS block_height_idx ON block (height)");
            case startNumber + 8:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("CREATE INDEX IF NOT EXISTS block_generator_id_idx ON block (generator_id)");
            case startNumber + 9:
                log.trace("Starting adding BLOCK constraint = {}", nextUpdate);
                apply("CREATE UNIQUE INDEX IF NOT EXISTS block_timestamp_idx ON block (timestamp DESC)");
/*  ---------------------- TRANSACTION -------------------    */
            case startNumber + 10:
                log.trace("Starting adding TRANSACTION constraint = {}", nextUpdate);
                apply("alter table TRANSACTION add constraint IF NOT EXISTS TRANSACTION_ID_IDX unique (ID)");
            case startNumber + 11:
                log.trace("Starting adding TRANSACTION constraint = {}", nextUpdate);
                apply("ALTER TABLE TRANSACTION ADD CONSTRAINT IF NOT EXISTS TRANSACTION_TO_BLOCK_FK FOREIGN KEY (block_id) REFERENCES block (id) ON DELETE CASCADE");
            case startNumber + 12:
                log.trace("Starting adding TRANSACTION constraint = {}", nextUpdate);
                apply("alter table TRANSACTION add constraint IF NOT EXISTS PRIMARY_KEY_TRANSACTION_DB_ID primary key (DB_ID)"); // PK + unique index
            case startNumber + 13:
                log.trace("Starting adding TRANSACTION constraint = {}", nextUpdate);
                apply("CREATE UNIQUE INDEX IF NOT EXISTS transaction_id_idx ON transaction (id)");
            case startNumber + 14:
                log.trace("Starting adding TRANSACTION constraint = {}", nextUpdate);
                apply("CREATE INDEX IF NOT EXISTS transaction_sender_id_idx ON transaction (sender_id)");
            case startNumber + 15:
                log.trace("Starting adding TRANSACTION constraint = {}", nextUpdate);
                apply("CREATE INDEX IF NOT EXISTS transaction_recipient_id_idx ON transaction (recipient_id)");
            case startNumber + 16:
                log.trace("Starting adding TRANSACTION constraint = {}", nextUpdate);
                apply("CREATE INDEX IF NOT EXISTS transaction_block_timestamp_idx ON transaction (block_timestamp DESC)");
            case startNumber + 17:
                log.trace("Starting adding TRANSACTION constraint = {}", nextUpdate);
                apply("CREATE INDEX IF NOT EXISTS referenced_transaction_referenced_transaction_id_idx ON referenced_transaction (referenced_transaction_id)");
            case startNumber + 18:
                return startNumber + 18;
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
