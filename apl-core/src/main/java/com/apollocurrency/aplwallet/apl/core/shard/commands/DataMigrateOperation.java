/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.commands;

import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;

/**
 * Common interface operations to be executed for creating shard database and internal data movement.
 *
 * @author yuriy.larin
 */
@FunctionalInterface
public interface DataMigrateOperation {

    int DEFAULT_COMMIT_BATCH_SIZE = 100;

    /**
     * Constant table names are used for step 2
     */
    String BLOCK_TABLE_NAME = "BLOCK";
    String TRANSACTION_TABLE_NAME = "TRANSACTION";


    /**
     * Constant table names are used for step 4
     */
    String GENESIS_PUBLIC_KEY_TABLE_NAME = "GENESIS_PUBLIC_KEY";
    String PUBLIC_KEY_TABLE_NAME = "PUBLIC_KEY";
    String TAGGED_DATA_TABLE_NAME = "TAGGED_DATA";
    String SHUFFLING_DATA_TABLE_NAME = "SHUFFLING_DATA";
    String DATA_TAG_TABLE_NAME = "DATA_TAG";
    String PRUNABLE_MESSAGE_TABLE_NAME = "PRUNABLE_MESSAGE";

    /**
     * Constant table names are used for step 5
     */
    String BLOCK_INDEX_TABLE_NAME = "BLOCK_INDEX";
    String TRANSACTION_SHARD_INDEX_TABLE_NAME = "TRANSACTION_SHARD_INDEX";

    /**
     * Execute sharding operations
     * @return migration result state enum value
     */
    MigrateState execute();

}
