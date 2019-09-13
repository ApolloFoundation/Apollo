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
    MAIN_DB_BACKUPED(1),

    /**
     * shard schema is created with only initial tables , no constraints/indexes
     */
    SHARD_SCHEMA_CREATED(2),

    /**
     * We started copying data (block + tr) from main into shard
     */
    DATA_COPY_TO_SHARD_STARTED(3),
    /**
     * We finished copying data (block + tr) from main into shard
     */
    DATA_COPY_TO_SHARD_FINISHED(4),

    /**
     * Shard schema is updated with all constraints/indexes, so it's FULLY created now.
     */
    SHARD_SCHEMA_FULL(5),

//    @Deprecated
//    DATA_RELINK_STARTED(6),

//    @Deprecated
//    DATA_RELINKED_IN_MAIN(7),

    /**
     * We started updating block/tr secondary indexes with shard's block related info
     */
    SECONDARY_INDEX_STARTED(6),
    /**
     * The block/tr secondary indexes were updated with shard related info
     */
    SECONDARY_INDEX_FINISHED(7),

    /**
     * We started export different tables data into CSV files
     */
    CSV_EXPORT_STARTED(8),
    /**
     * CSV export tables data into CSV is finished
     */
    CSV_EXPORT_FINISHED(9),

    /**
     * We started archiving CSV files into ZIP and putting CRC/hash on last step
     */
    ZIP_ARCHIVE_STARTED(10),
    /**
     * Creating ZIP archive with CSV files is finished
     */
    ZIP_ARCHIVE_FINISHED(11),

    /**
     * We started deleting Block/tr data from main db after it has been copied to shard db
     */
    DATA_REMOVE_STARTED(12),
    /**
     * Block/tr data were deleted from main db after it has been copied to shard db
     */
    DATA_REMOVED_FROM_MAIN(13),

    /**
     * Shard record is inserted in main db and sharding process is completed
     */
    COMPLETED(14),
    /**
     * Process failed at any step
     */
    FAILED(-1);

    private final int id; // internal value for compare operation

    MigrateState(int id) { this.id = id; }

    public int getValue() { return id; }
}
