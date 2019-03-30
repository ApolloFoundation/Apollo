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
 * Update records in specified tables so they point to snapshot block in main db
 */
public class ReLinkDataCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(ReLinkDataCommand.class);

    private DataTransferManagementReceiver dataTransferManagement;
    private List<String> tableNameList;
    private int commitBatchSize;
    private long snapshotBlockHeight;
    private Set<Long> dbIdsExclusionList;

    public ReLinkDataCommand(DataTransferManagementReceiver dataTransferManagement,
                             int commitBatchSize, long snapshotBlockHeight, List<String> tableNameList, Set<Long> dbIdsExlusionList) {
        this.dataTransferManagement = Objects.requireNonNull(dataTransferManagement, "dataTransferManagement is NULL");
        this.snapshotBlockHeight = snapshotBlockHeight;
        this.commitBatchSize = commitBatchSize <= 0 ? DEFAULT_COMMIT_BATCH_SIZE : commitBatchSize;
        this.dbIdsExclusionList = dbIdsExlusionList == null ? Collections.emptySet() : dbIdsExlusionList;
        this.tableNameList = tableNameList == null ? new ArrayList<>() : tableNameList;
    }

    public ReLinkDataCommand(DataTransferManagementReceiver dataTransferManagement,
                             long snapshotBlockHeight, Set<Long> dbIdsExclusionList) {
        this(dataTransferManagement,  null, DEFAULT_COMMIT_BATCH_SIZE, snapshotBlockHeight, dbIdsExclusionList);
        //TODO move it to another class
        tableNameList.add(TRANSACTION_TABLE_NAME);
        tableNameList.add(PUBLIC_KEY_TABLE_NAME);
        tableNameList.add(TAGGED_DATA_TABLE_NAME);
        tableNameList.add(SHUFFLING_DATA_TABLE_NAME);
        tableNameList.add(DATA_TAG_TABLE_NAME);
        tableNameList.add(PRUNABLE_MESSAGE_TABLE_NAME);
    }

    public ReLinkDataCommand(
            DataTransferManagementReceiver dataTransferManagement,
            List<String> tableNameList,
            int commitBatchSize,
            Long snapshotBlockHeight, Set<Long> dbIdsExclusionList) {
        this(dataTransferManagement, commitBatchSize, snapshotBlockHeight, tableNameList, dbIdsExclusionList);
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
        return dataTransferManagement.relinkDataToSnapshotBlock(paramInfo);
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
