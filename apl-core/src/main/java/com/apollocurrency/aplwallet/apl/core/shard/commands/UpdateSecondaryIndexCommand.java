/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.commands;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.shard.DataTransferManagementReceiver;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import org.slf4j.Logger;

/**
 * Update block/tr secondary tables in main db.
 */
public class UpdateSecondaryIndexCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(UpdateSecondaryIndexCommand.class);

    private DataTransferManagementReceiver dataTransferManagement;
    private List<String> tableNameList = new LinkedList<>();
    private int commitBatchSize = DEFAULT_COMMIT_BATCH_SIZE;
    private long snapshotBlockHeight = 0L;

    public UpdateSecondaryIndexCommand(DataTransferManagementReceiver dataTransferManagement,
                                       int commitBatchSize, long snapshotBlockHeight) {
        this(dataTransferManagement, snapshotBlockHeight);
        this.commitBatchSize = commitBatchSize;
    }

    public UpdateSecondaryIndexCommand(DataTransferManagementReceiver dataTransferManagement,
                                       long snapshotBlockHeight) {
        this.dataTransferManagement = Objects.requireNonNull(dataTransferManagement, "dataTransferManagement is NULL");
        this.snapshotBlockHeight = snapshotBlockHeight;
        tableNameList.add(BLOCK_INDEX_TABLE_NAME);
        tableNameList.add(TRANSACTION_SHARD_INDEX_TABLE_NAME);
    }

    public UpdateSecondaryIndexCommand(
            DataTransferManagementReceiver dataTransferManagement,
            List<String> tableNameList,
            int commitBatchSize,
            Long snapshotBlockHeight) {
        this.dataTransferManagement = Objects.requireNonNull(dataTransferManagement, "dataTransferManagement is NULL");
        this.tableNameList = Objects.requireNonNull(tableNameList, "tableNameList is NULL");
        this.commitBatchSize = commitBatchSize;
        this.snapshotBlockHeight = snapshotBlockHeight;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState execute() {
        log.debug("Update Secondary Index Data Command execute...");
        CommandParamInfo paramInfo = new CommandParamInfoImpl(
                this.tableNameList, this.commitBatchSize, this.snapshotBlockHeight);
        return dataTransferManagement.updateSecondaryIndex(paramInfo);
    }
}
