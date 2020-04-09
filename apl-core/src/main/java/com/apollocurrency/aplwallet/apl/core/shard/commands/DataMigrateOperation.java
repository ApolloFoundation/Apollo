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

    /**
     * Execute sharding operations
     *
     * @return migration result state enum value
     */
    MigrateState execute();

}
