package com.apollocurrency.aplwallet.apl.core.shard.commands;

import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;

@FunctionalInterface
public interface DataMigrateOperation {

    MigrateState execute();

}
