/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.commands;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.shard.ExcludeInfo;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.core.shard.ShardConstants;
import com.apollocurrency.aplwallet.apl.core.shard.ShardEngine;
import com.apollocurrency.aplwallet.apl.util.StringValidator;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Command copy block + transaction data from main into shard database.
 *
 * @author yuriy.larin
 */
public class CopyDataCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(CopyDataCommand.class);

    private ShardEngine shardEngine;
    private List<String> tableNameList;
    private int commitBatchSize;
    private int snapshotBlockHeight;
    private ExcludeInfo excludeInfo;

    public CopyDataCommand(ShardEngine shardEngine,
                           int commitBatchSize, int snapshotBlockHeight, ExcludeInfo excludeInfo) {
        this(shardEngine, null, commitBatchSize, snapshotBlockHeight, excludeInfo);
    }

    public CopyDataCommand(ShardEngine shardEngine,
                           int snapshotBlockHeight, ExcludeInfo excludeInfo) {
        this(shardEngine, ShardConstants.DEFAULT_COMMIT_BATCH_SIZE, snapshotBlockHeight, excludeInfo);
        tableNameList.add(ShardConstants.BLOCK_TABLE_NAME);
        tableNameList.add(ShardConstants.TRANSACTION_TABLE_NAME);
    }

    public CopyDataCommand(
            ShardEngine shardEngine,
            List<String> tableNameList,
            int commitBatchSize, int snapshotBlockHeight, ExcludeInfo excludeInfo) {
        this.shardEngine = Objects.requireNonNull(shardEngine, "shardEngine is NULL");
        this.tableNameList = tableNameList == null ? new ArrayList<>() :tableNameList;
        this.commitBatchSize = commitBatchSize;
        this.snapshotBlockHeight = snapshotBlockHeight;
        this.excludeInfo = excludeInfo;
    }

    public void addTable(String table) {
        StringValidator.requireNonBlank(table);
        tableNameList.add(table);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState execute() {
        log.debug("Copy Shard Data Command execute...");
        CommandParamInfo paramInfo = new CommandParamInfoImpl(
                this.tableNameList, this.commitBatchSize, this.snapshotBlockHeight, this.excludeInfo);
        return shardEngine.copyDataToShard(paramInfo);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("CopyDataCommand{");
        sb.append("tableNameList=").append(tableNameList);
        sb.append(", commitBatchSize=").append(commitBatchSize);
        sb.append(", snapshotBlockHeight=").append(snapshotBlockHeight);
        sb.append('}');
        return sb.toString();
    }
}
