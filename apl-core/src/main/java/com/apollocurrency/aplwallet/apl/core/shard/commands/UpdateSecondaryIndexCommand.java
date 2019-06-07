/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.commands;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.shard.ShardConstants;
import com.apollocurrency.aplwallet.apl.core.shard.ShardEngine;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.util.StringValidator;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Update block/tr secondary tables in main db.
 */
public class UpdateSecondaryIndexCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(UpdateSecondaryIndexCommand.class);

    private ShardEngine shardEngine;
    private List<String> tableNameList;
    private int commitBatchSize;
    private int snapshotBlockHeight;
    private Set<Long> dbIdsExclusionList;

    public UpdateSecondaryIndexCommand(ShardEngine shardEngine,
                                       int commitBatchSize, int snapshotBlockHeight, List<String> tableNameList, Set<Long> dbIdsExlusionList) {
        this.shardEngine = Objects.requireNonNull(shardEngine, "shardEngine is NULL");
        this.snapshotBlockHeight = snapshotBlockHeight;
        this.commitBatchSize = commitBatchSize <= 0 ? ShardConstants.DEFAULT_COMMIT_BATCH_SIZE : commitBatchSize;
        this.dbIdsExclusionList = dbIdsExlusionList == null ? Collections.emptySet() : dbIdsExlusionList;
        this.tableNameList = tableNameList == null ? new ArrayList<>() : tableNameList;
    }

    public UpdateSecondaryIndexCommand(ShardEngine shardEngine,
                                       int snapshotBlockHeight, Set<Long> dbIdsExclusionList) {
        this(shardEngine,  ShardConstants.DEFAULT_COMMIT_BATCH_SIZE, snapshotBlockHeight, dbIdsExclusionList);
        tableNameList.add(ShardConstants.BLOCK_INDEX_TABLE_NAME);
        tableNameList.add(ShardConstants.TRANSACTION_INDEX_TABLE_NAME);
    }

    public UpdateSecondaryIndexCommand(
            ShardEngine shardEngine,
            int commitBatchSize,
            int snapshotBlockHeight, Set<Long> dbIdsExclusionList) {
        this(shardEngine, commitBatchSize, snapshotBlockHeight, null, dbIdsExclusionList);
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
        log.debug("Update Secondary Index Data Command execute...");
        CommandParamInfo paramInfo = new CommandParamInfoImpl(
                this.tableNameList, this.commitBatchSize, this.snapshotBlockHeight, this.dbIdsExclusionList);
        return shardEngine.updateSecondaryIndex(paramInfo);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("UpdateSecondaryIndexCommand{");
        sb.append("tableNameList=").append(tableNameList);
        sb.append(", commitBatchSize=").append(commitBatchSize);
        sb.append(", snapshotBlockHeight=").append(snapshotBlockHeight);
        sb.append('}');
        return sb.toString();
    }
}
