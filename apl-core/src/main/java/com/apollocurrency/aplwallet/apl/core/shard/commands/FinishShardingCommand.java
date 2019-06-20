/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.commands;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.core.shard.ShardEngine;
import org.slf4j.Logger;

/**
 * Command for updating Shard table in main database. Inserts record about just created shard.
 *
 * @author yuriy.larin
 */
public class FinishShardingCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(FinishShardingCommand.class);

    private ShardEngine shardEngine;


    public FinishShardingCommand(ShardEngine shardEngine) {
        this.shardEngine = Objects.requireNonNull(
                shardEngine, "shardEngine is NULL");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState execute() {
        log.debug("Finish Sharding Command execute...");

        CommandParamInfo paramInfo = new CommandParamInfoImpl();

        return shardEngine.finishShardProcess(paramInfo);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("FinishShardingCommand");
        return sb.toString();
    }
}
