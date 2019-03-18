/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.commands;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.shard.DataTransferManagementReceiver;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import org.slf4j.Logger;

/**
 * Command for updating Shard table in main database. Inserts record about just created shard.
 *
 * @author yuriy.larin
 */
public class FinishShardingCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(FinishShardingCommand.class);

    private DataTransferManagementReceiver dataTransferManagement;
    private byte[] shardHash;

    public FinishShardingCommand(DataTransferManagementReceiver dataTransferManagement) {
        this.dataTransferManagement = Objects.requireNonNull(
                dataTransferManagement, "dataTransferManagement is NULL");
    }

    public FinishShardingCommand(DataTransferManagementReceiver dataTransferManagement, byte[] shardHash) {
        this.dataTransferManagement = Objects.requireNonNull(
                dataTransferManagement, "dataTransferManagement is NULL");
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
        return dataTransferManagement.addShardInfo(paramInfo);
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
