/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.commands;

import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.core.shard.ShardEngine;
import com.apollocurrency.aplwallet.apl.core.shard.model.TableInfo;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Command archive all CSV data into zip and calculate CRC/Hash.
 *
 * @author yuriy.larin
 */
public class ZipArchiveCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(ZipArchiveCommand.class);

    private ShardEngine shardEngine;
    private long shardId;
    private List<TableInfo> tableInfoList;

    public ZipArchiveCommand(long shardId, List<TableInfo> tableInfoList, ShardEngine shardEngine) {
        this.shardEngine = Objects.requireNonNull(
                shardEngine, "shardEngine is NULL");
        this.tableInfoList = Objects.requireNonNull(tableInfoList, "tableInfoList is null");
        this.shardId = shardId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState execute() {
        log.debug("Finish Sharding Command execute...");
        // in reality the zip CRC is computed inside ShardEngineImpl
        CommandParamInfo paramInfo = CommandParamInfo.builder().shardId(shardId).tableInfoList(tableInfoList).build();
        return shardEngine.archiveCsv(paramInfo);
    }

    @Override
    public String toString() {
        return "ZipArchiveCommand";
    }
}
