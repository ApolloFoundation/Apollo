package com.apollocurrency.aplwallet.apl.core.shard;

public enum MigrateState {
    INIT, SHARD_DB_CREATED, SNAPSHOT_BLOCK_CREATED, DATA_MOVING_STARTED, DATA_MOVED, FILE_REMANING, COMPLETED, FAILED;
}
