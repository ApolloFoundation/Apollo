/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.commands;

import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.core.shard.ShardEngine;
import org.slf4j.Logger;

import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Command for updating Shard table in main database. Inserts record about just created shard.
 *
 * @author yuriy.larin
 */
public class FinishShardingCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(FinishShardingCommand.class);

    private ShardEngine shardEngine;
    private long shardId;


    public FinishShardingCommand(ShardEngine shardEngine, long shardId) {
        this.shardEngine = Objects.requireNonNull(
            shardEngine, "shardEngine is NULL");
        this.shardId = shardId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState execute() {
        log.debug("Finish Sharding Command execute...");

        CommandParamInfo paramInfo = CommandParamInfo.builder().shardId(shardId).build();

        return shardEngine.finishShardProcess(paramInfo);
    }

    @Override
    public String toString() {
        return "FinishShardingCommand-" + shardId;

    }
}
