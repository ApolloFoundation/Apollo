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

public class DeleteCopiedDataCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(DeleteCopiedDataCommand.class);

    private ShardEngine shardEngine;
    private List<TableInfo> tableNameList = new ArrayList<>();
    private int commitBatchSize = ShardConstants.DEFAULT_COMMIT_BATCH_SIZE;
    private ExcludeInfo excludeInfo;
    private int snapshotBlockHeight;

    public DeleteCopiedDataCommand(ShardEngine shardEngine,
                                   int commitBatchSize, int snapshotBlockHeight, ExcludeInfo excludeInfo) {
        this(shardEngine, snapshotBlockHeight, excludeInfo);
        this.commitBatchSize = commitBatchSize;
    }

    public DeleteCopiedDataCommand(ShardEngine shardEngine,
                                   int snapshotBlockHeight, ExcludeInfo excludeInfo) {
        this.shardEngine = Objects.requireNonNull(shardEngine, "shardEngine is NULL");
        this.snapshotBlockHeight = snapshotBlockHeight;
        this.excludeInfo = Objects.requireNonNull(excludeInfo, "excludeInfo set is NULL");
        tableNameList.add(new TableInfo(ShardConstants.BLOCK_TABLE_NAME));
        tableNameList.add(new TableInfo(ShardConstants.TRANSACTION_TABLE_NAME));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState execute() {
        log.debug("Delete Block/Transaction Data from main DB Command execute...");
        CommandParamInfo paramInfo = CommandParamInfo.builder()
            .tableInfoList(this.tableNameList)
            .commitBatchSize(this.commitBatchSize)
            .snapshotBlockHeight(this.snapshotBlockHeight)
            .excludeInfo(this.excludeInfo)
            .build();
        return shardEngine.deleteCopiedData(paramInfo);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("DeleteCopiedDataCommand{");
        sb.append("tableInfoList=").append(tableNameList);
        sb.append(", commitBatchSize=").append(commitBatchSize);
        sb.append(", snapshotBlockHeight=").append(snapshotBlockHeight);
        sb.append('}');
        return sb.toString();
    }
}
