package com.apollocurrency.aplwallet.apl.core.shard;

public enum MigrateState {
    INIT, TEMP_DB_CREATED, DATA_MOVING, FILE_REMANING, COMPLETED, FAILED;
}
