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
 * Command archive all CSV data into zip and calculate CRC/Hash.
 *
 * @author yuriy.larin
 */
public class ZipArchiveCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(ZipArchiveCommand.class);

    private ShardEngine shardEngine;
    private byte[] zipCrcHash; // TODO: not correct now

    public ZipArchiveCommand(ShardEngine shardEngine) {
        this.shardEngine = Objects.requireNonNull(
                shardEngine, "shardEngine is NULL");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState execute() {
        log.debug("Finish Sharding Command execute...");
        // TODO: constructor is not correct here, because zipCrc will be computed inside and passed along (stored in DB)
        CommandParamInfo paramInfo = new CommandParamInfoImpl(this.zipCrcHash, true);
        return shardEngine.archiveCsv(paramInfo);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("FinishShardingCommand{");
        sb.append("zipCrcHash=");
        if (zipCrcHash == null) sb.append("null");
        else {
            sb.append('[');
            sb.append(zipCrcHash.length);
            sb.append(']');
        }
        sb.append('}');
        return sb.toString();
    }
}
