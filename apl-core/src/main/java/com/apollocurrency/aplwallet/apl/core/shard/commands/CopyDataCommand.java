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
 * Command copy block + transaction data from main into shard database.
 *
 * @author yuriy.larin
 */
public class CopyDataCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(CopyDataCommand.class);

    private DataTransferManagementReceiver dataTransferManagement;
    private List<String> tableNameList;
    private int commitBatchSize;
    private long snapshotBlockHeight;
    private Set<Long> dbIdsExclusionList;

    public CopyDataCommand(DataTransferManagementReceiver dataTransferManagement,
                           int commitBatchSize, long snapshotBlockHeight, Set<Long> dbIdsExclusionList) {
        this(dataTransferManagement, null, commitBatchSize, snapshotBlockHeight, dbIdsExclusionList);
    }

    public CopyDataCommand(DataTransferManagementReceiver dataTransferManagement,
                           long snapshotBlockHeight, Set<Long> dbIdsExclusionList) {
        this(dataTransferManagement, DEFAULT_COMMIT_BATCH_SIZE, snapshotBlockHeight, dbIdsExclusionList);
        tableNameList.add(BLOCK_TABLE_NAME);
        tableNameList.add(TRANSACTION_TABLE_NAME);
    }

    public CopyDataCommand(
            DataTransferManagementReceiver dataTransferManagement,
            List<String> tableNameList,
            int commitBatchSize,
            Long snapshotBlockHeight, Set<Long> dbIdsExclusionList) {
        this.dataTransferManagement = Objects.requireNonNull(dataTransferManagement, "dataTransferManagement is NULL");
        this.tableNameList = tableNameList == null ? new ArrayList<>() :tableNameList;
        this.commitBatchSize = commitBatchSize;
        this.snapshotBlockHeight = snapshotBlockHeight;
        this.dbIdsExclusionList = dbIdsExclusionList == null ? Collections.emptySet() : dbIdsExclusionList;
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
                this.tableNameList, this.commitBatchSize, this.snapshotBlockHeight, this.dbIdsExclusionList);
        return dataTransferManagement.copyDataToShard(paramInfo);
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
