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
 * Update block/tr secondary tables in main db.
 */
public class UpdateSecondaryIndexCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(UpdateSecondaryIndexCommand.class);

    private ShardEngine shardEngine;
    private List<TableInfo> tableNameList;
    private int commitBatchSize;
    private int snapshotBlockHeight;
    private ExcludeInfo excludeInfo;

    public UpdateSecondaryIndexCommand(ShardEngine shardEngine,
                                       int commitBatchSize, int snapshotBlockHeight, List<TableInfo> tableNameList, ExcludeInfo excludeInfo) {
        this.shardEngine = Objects.requireNonNull(shardEngine, "shardEngine is NULL");
        this.snapshotBlockHeight = snapshotBlockHeight;
        this.commitBatchSize = commitBatchSize <= 0 ? ShardConstants.DEFAULT_COMMIT_BATCH_SIZE : commitBatchSize;
        this.excludeInfo = excludeInfo;
        this.tableNameList = tableNameList == null ? new ArrayList<>() : tableNameList;
    }

    public UpdateSecondaryIndexCommand(ShardEngine shardEngine,
                                       int snapshotBlockHeight, ExcludeInfo excludeInfo) {
        this(shardEngine, ShardConstants.DEFAULT_COMMIT_BATCH_SIZE, snapshotBlockHeight, excludeInfo);
        tableNameList.add(new TableInfo(ShardConstants.BLOCK_INDEX_TABLE_NAME));
        tableNameList.add(new TableInfo(ShardConstants.TRANSACTION_INDEX_TABLE_NAME));
    }

    public UpdateSecondaryIndexCommand(
        ShardEngine shardEngine,
        int commitBatchSize,
        int snapshotBlockHeight, ExcludeInfo excludeInfo) {
        this(shardEngine, commitBatchSize, snapshotBlockHeight, null, excludeInfo);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState execute() {
        log.debug("Update Secondary Index Data Command execute...");
        CommandParamInfo paramInfo = CommandParamInfo.builder()
            .tableInfoList(this.tableNameList)
            .commitBatchSize(this.commitBatchSize)
            .snapshotBlockHeight(this.snapshotBlockHeight)
            .excludeInfo(this.excludeInfo)
            .build();
        return shardEngine.updateSecondaryIndex(paramInfo);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("UpdateSecondaryIndexCommand{");
        sb.append("tableInfoList=").append(tableNameList);
        sb.append(", commitBatchSize=").append(commitBatchSize);
        sb.append(", snapshotBlockHeight=").append(snapshotBlockHeight);
        sb.append('}');
        return sb.toString();
    }
}
