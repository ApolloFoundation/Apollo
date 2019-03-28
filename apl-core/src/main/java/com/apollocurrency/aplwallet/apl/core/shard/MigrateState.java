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
    INIT(0),
    /**
     * shard schema is created with only initial tables , no constraints/indexes
     */
    SHARD_SCHEMA_CREATED(1),

    /**
     * We started copying data (block + tr) from main into shard
     */
    DATA_COPY_TO_SHARD_STARTED(2),
    /**
     * We finished copying data (block + tr) from main into shard
     */
    DATA_COPIED_TO_SHARD(3),

    /**
     * Shard schema is updated with all constraints/indexes, so it's FULLY created now.
     */
    SHARD_SCHEMA_FULL(4),

    /**
     * We started updating several tables to link/reference to shard's snapshot block
     */
    DATA_RELINK_STARTED(5),
    /**
     * Several tables were processed and records were updated to reference shard's snapshot block
     */
    DATA_RELINKED_IN_MAIN(6),

    /**
     * We started updating block/tr secondary indexes with shard's block related info
     */
    SECONDARY_INDEX_STARTED(7),
    /**
     * The block/tr secondary indexes were updated with shard related info
     */
    SECONDARY_INDEX_UPDATED(8),

    /**
     * We started deleting Block/tr data from main db after it has been copied to shard db
     */
    DATA_REMOVE_STARTED(9),
    /**
     * Block/tr data were deleted from main db after it has been copied to shard db
     */
    DATA_REMOVED_FROM_MAIN(10),

    /**
     * Shard record is inserted in main db and sharding process is completed
     */
    COMPLETED(11),
    /**
     * Process failed at any step
     */
    FAILED(-1);

    private final int id; // internal value for compare operation

    MigrateState(int id) { this.id = id; }

    public int getValue() { return id; }
}
