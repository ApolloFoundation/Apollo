package com.apollocurrency.aplwallet.apl.core.shard.commands;

import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;

/**
 * Common interface for all operations
 */
@FunctionalInterface
public interface DataMigrateOperation {

    MigrateState execute();

}
