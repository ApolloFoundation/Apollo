/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

/**
 * Enum used for tracking migration state on all shard operation steps.
 */
public enum MigrateState {
    /**
     * started shard schema, but it was finished yet
     */
    INIT,
    /**
     * shard schema is created with only initial tables , no constraints/indexes
     */
    SHARD_SCHEMA_CREATED,
    /**
     * We started copying data (block + tr) from main into shard
     */
    DATA_COPY_TO_SHARD_STARTED,
    /**
     * We finished copying data (block + tr) from main into shard
     */
    DATA_COPIED_TO_SHARD,
    /**
     * Shard schema is updated with all constraints/indexes
     */
    SHARD_SCHEMA_FULL,
    /**
     * Several tables were processed and records were updated to reference shard's snapshot block
     */
    DATA_RELINKED_IN_MAIN,
    /**
     * The block/tr secondary indexes were updated with shard related info
     */
    SECONDARY_INDEX_UPDATED,
    /**
     * Block/tr data were deleted from main db after it has beed copied to shard db
     */
    DATA_REMOVED_FROM_MAIN,
    /**
     * Shard record is inserted in main db and sharding process is completed
     */
    COMPLETED,
    /**
     * Process failed at any step
     */
    FAILED;
}
