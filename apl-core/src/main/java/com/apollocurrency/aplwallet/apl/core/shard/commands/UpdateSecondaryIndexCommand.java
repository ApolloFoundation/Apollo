/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.commands;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.shard.DataTransferManagementReceiver;
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

    private DataTransferManagementReceiver dataTransferManagement;
    private List<String> tableNameList;
    private int commitBatchSize;
    private long snapshotBlockHeight;
    private Set<Long> dbIdsExclusionList;

    public UpdateSecondaryIndexCommand(DataTransferManagementReceiver dataTransferManagement,
                             int commitBatchSize, long snapshotBlockHeight, List<String> tableNameList, Set<Long> dbIdsExlusionList) {
        this.dataTransferManagement = Objects.requireNonNull(dataTransferManagement, "dataTransferManagement is NULL");
        this.snapshotBlockHeight = snapshotBlockHeight;
        this.commitBatchSize = commitBatchSize <= 0 ? DEFAULT_COMMIT_BATCH_SIZE : commitBatchSize;
        this.dbIdsExclusionList = dbIdsExlusionList == null ? Collections.emptySet() : dbIdsExlusionList;
        this.tableNameList = tableNameList == null ? new ArrayList<>() : tableNameList;
    }

    public UpdateSecondaryIndexCommand(DataTransferManagementReceiver dataTransferManagement,
                             long snapshotBlockHeight, Set<Long> dbIdsExclusionList) {
        this(dataTransferManagement,  DEFAULT_COMMIT_BATCH_SIZE, snapshotBlockHeight, dbIdsExclusionList);
        tableNameList.add(BLOCK_INDEX_TABLE_NAME);
        tableNameList.add(TRANSACTION_SHARD_INDEX_TABLE_NAME);

    }

    public UpdateSecondaryIndexCommand(
            DataTransferManagementReceiver dataTransferManagement,
            int commitBatchSize,
            Long snapshotBlockHeight, Set<Long> dbIdsExclusionList) {
        this(dataTransferManagement, commitBatchSize, snapshotBlockHeight, null, dbIdsExclusionList);
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
        return dataTransferManagement.updateSecondaryIndex(paramInfo);
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
