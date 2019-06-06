/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.commands;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.shard.ShardConstants;
import com.apollocurrency.aplwallet.apl.core.shard.ShardEngine;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class DeleteCopiedDataCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(DeleteCopiedDataCommand.class);

    private ShardEngine shardEngine;
    private List<String> tableNameList = new ArrayList<>();
    private int commitBatchSize = ShardConstants.DEFAULT_COMMIT_BATCH_SIZE;
    private Set<Long> excludedTxs;
    private int snapshotBlockHeight;

    public DeleteCopiedDataCommand(ShardEngine shardEngine,
                                   int commitBatchSize, int snapshotBlockHeight, Set<Long> excludedTxs) {
        this(shardEngine, snapshotBlockHeight, excludedTxs);
        this.commitBatchSize = commitBatchSize;
    }

    public DeleteCopiedDataCommand(ShardEngine shardEngine,
                                   int snapshotBlockHeight, Set<Long> excludedTxs) {
        this.shardEngine = Objects.requireNonNull(shardEngine, "shardEngine is NULL");
        this.snapshotBlockHeight = snapshotBlockHeight;
        this.excludedTxs = Objects.requireNonNull(excludedTxs, "excludedTxs set is NULL");
        tableNameList.add(ShardConstants.BLOCK_TABLE_NAME);
        tableNameList.add(ShardConstants.TRANSACTION_TABLE_NAME);
    }

    public DeleteCopiedDataCommand(
            ShardEngine shardEngine,
            List<String> tableNameList,
            int commitBatchSize,
            int snapshotBlockHeight) {
        this.shardEngine = Objects.requireNonNull(shardEngine, "shardEngine is NULL");
        this.tableNameList = Objects.requireNonNull(tableNameList, "tableNameList is NULL");
        this.commitBatchSize = commitBatchSize;
        this.snapshotBlockHeight = snapshotBlockHeight;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState execute() {
        log.debug("Delete Block/Transaction Data from main DB Command execute...");
        CommandParamInfo paramInfo = new CommandParamInfoImpl(
                this.tableNameList, this.commitBatchSize, this.snapshotBlockHeight, excludedTxs);
        return shardEngine.deleteCopiedData(paramInfo);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("DeleteCopiedDataCommand{");
        sb.append("tableNameList=").append(tableNameList);
        sb.append(", commitBatchSize=").append(commitBatchSize);
        sb.append(", snapshotBlockHeight=").append(snapshotBlockHeight);
        sb.append('}');
        return sb.toString();
    }
}
