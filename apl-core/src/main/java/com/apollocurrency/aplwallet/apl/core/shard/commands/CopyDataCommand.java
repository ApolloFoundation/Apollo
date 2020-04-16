/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.commands;

import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.core.shard.ShardConstants;
import com.apollocurrency.aplwallet.apl.core.shard.ShardEngine;
import com.apollocurrency.aplwallet.apl.core.shard.model.ExcludeInfo;
import com.apollocurrency.aplwallet.apl.core.shard.model.TableInfo;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Command copy block + transaction data from main into shard database.
 *
 * @author yuriy.larin
 */
public class CopyDataCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(CopyDataCommand.class);

    private ShardEngine shardEngine;
    private List<TableInfo> tableNameList;
    private int commitBatchSize;
    private int snapshotBlockHeight;
    private ExcludeInfo excludeInfo;
    private long shardId;

    public CopyDataCommand(long shardId, ShardEngine shardEngine,
                           int commitBatchSize, int snapshotBlockHeight, ExcludeInfo excludeInfo) {
        this(shardId, shardEngine, null, commitBatchSize, snapshotBlockHeight, excludeInfo);
    }

    public CopyDataCommand(long shardId, ShardEngine shardEngine,
                           int snapshotBlockHeight, ExcludeInfo excludeInfo) {
        this(shardId, shardEngine, ShardConstants.DEFAULT_COMMIT_BATCH_SIZE, snapshotBlockHeight, excludeInfo);
        tableNameList.add(new TableInfo(ShardConstants.BLOCK_TABLE_NAME));
        tableNameList.add(new TableInfo(ShardConstants.TRANSACTION_TABLE_NAME));
    }

    public CopyDataCommand(
        long shardId,
        ShardEngine shardEngine,
        List<TableInfo> tableInfoList,
        int commitBatchSize, int snapshotBlockHeight, ExcludeInfo excludeInfo) {
        this.shardId = shardId;
        this.shardEngine = Objects.requireNonNull(shardEngine, "shardEngine is NULL");
        this.tableNameList = tableInfoList == null ? new ArrayList<>() : tableInfoList;
        this.commitBatchSize = commitBatchSize;
        this.snapshotBlockHeight = snapshotBlockHeight;
        this.excludeInfo = excludeInfo;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState execute() {
        log.debug("Copy Shard Data Command execute...");
        CommandParamInfo paramInfo = CommandParamInfo.builder()
            .tableInfoList(this.tableNameList)
            .commitBatchSize(this.commitBatchSize)
            .snapshotBlockHeight(this.snapshotBlockHeight)
            .excludeInfo(this.excludeInfo)
            .shardId(shardId)
            .build();
        return shardEngine.copyDataToShard(paramInfo);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("CopyDataCommand{");
        sb.append("tableInfoList=").append(tableNameList);
        sb.append(", commitBatchSize=").append(commitBatchSize);
        sb.append(", snapshotBlockHeight=").append(snapshotBlockHeight);
        sb.append('}');
        return sb.toString();
    }
}
