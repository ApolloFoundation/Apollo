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
 * Update records in specified tables so they point to snapshot block in main db
 */
@Deprecated
public class ReLinkDataCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(ReLinkDataCommand.class);

    private ShardEngine shardEngine;
    private List<String> tableNameList;
    private int commitBatchSize;
    private int snapshotBlockHeight;
    private Set<Long> dbIdsExclusionList;

    public ReLinkDataCommand(ShardEngine shardEngine,
                             int commitBatchSize, int snapshotBlockHeight, List<String> tableNameList, Set<Long> dbIdsExclusionList) {
        this.shardEngine = Objects.requireNonNull(shardEngine, "shardEngine is NULL");
        this.snapshotBlockHeight = snapshotBlockHeight;
        this.commitBatchSize = commitBatchSize <= 0 ? ShardConstants.DEFAULT_COMMIT_BATCH_SIZE : commitBatchSize;
        this.dbIdsExclusionList = dbIdsExclusionList == null ? Collections.emptySet() : dbIdsExclusionList;
        this.tableNameList = tableNameList == null ? new ArrayList<>() : tableNameList;
    }

    public ReLinkDataCommand(ShardEngine shardEngine,
                             int snapshotBlockHeight, Set<Long> dbIdsExclusionList) {
        this(shardEngine,  null, ShardConstants.DEFAULT_COMMIT_BATCH_SIZE, snapshotBlockHeight, dbIdsExclusionList);
        //TODO move it to another class
/*
        tableNameList.add(TRANSACTION_TABLE_NAME);
        tableNameList.add(PUBLIC_KEY_TABLE_NAME);
        tableNameList.add(TAGGED_DATA_TABLE_NAME);
        tableNameList.add(SHUFFLING_DATA_TABLE_NAME);
        tableNameList.add(DATA_TAG_TABLE_NAME);
        tableNameList.add(PRUNABLE_MESSAGE_TABLE_NAME);
*/
    }

    public ReLinkDataCommand(
            ShardEngine shardEngine,
            List<String> tableNameList,
            int commitBatchSize,
            int snapshotBlockHeight, Set<Long> dbIdsExclusionList) {
        this(shardEngine, commitBatchSize, snapshotBlockHeight, tableNameList, dbIdsExclusionList);
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
        log.debug("Update Linked Data Command execute...");
        CommandParamInfo paramInfo = new CommandParamInfoImpl(
                this.tableNameList, this.commitBatchSize, this.snapshotBlockHeight, dbIdsExclusionList);
//        return shardEngine.relinkDataToSnapshotBlock(paramInfo);
        return null;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ReLinkDataCommand{");
        sb.append("tableNameList=").append(tableNameList);
        sb.append(", commitBatchSize=").append(commitBatchSize);
        sb.append(", snapshotBlockHeight=").append(snapshotBlockHeight);
        sb.append('}');
        return sb.toString();
    }
}
