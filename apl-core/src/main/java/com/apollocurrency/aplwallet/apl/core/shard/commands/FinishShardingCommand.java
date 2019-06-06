/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.commands;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.shard.ShardEngine;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import org.slf4j.Logger;

/**
 * Command for updating Shard table in main database. Inserts record about just created shard.
 *
 * @author yuriy.larin
 */
public class FinishShardingCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(FinishShardingCommand.class);

    private ShardEngine shardEngine;
    private byte[] shardHash;

    public FinishShardingCommand(ShardEngine shardEngine) {
        this.shardEngine = Objects.requireNonNull(
                shardEngine, "shardEngine is NULL");
    }

    public FinishShardingCommand(ShardEngine shardEngine, byte[] shardHash) {
        this.shardEngine = Objects.requireNonNull(
                shardEngine, "shardEngine is NULL");
        this.shardHash = Objects.requireNonNull(
                shardHash, "shardHash is NULL");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState execute() {
        log.debug("Finish Sharding Command execute...");
        CommandParamInfo paramInfo = new CommandParamInfoImpl(this.shardHash);
        return shardEngine.addShardHashInfo(paramInfo);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("FinishShardingCommand{");
        sb.append("shardHash=");
        if (shardHash == null) sb.append("null");
        else {
            sb.append('[');
            sb.append(shardHash.length);
            sb.append(']');
        }
        sb.append('}');
        return sb.toString();
    }
}
