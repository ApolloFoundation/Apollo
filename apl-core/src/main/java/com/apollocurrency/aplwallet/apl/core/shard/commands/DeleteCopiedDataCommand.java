/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.commands;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.shard.DataTransferManagementReceiver;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DeleteCopiedDataCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(DeleteCopiedDataCommand.class);

    private DataTransferManagementReceiver dataTransferManagement;
    private List<String> tableNameList = new ArrayList<>();
    private int commitBatchSize = DEFAULT_COMMIT_BATCH_SIZE;
    private long snapshotBlockHeight;

    public DeleteCopiedDataCommand(DataTransferManagementReceiver dataTransferManagement,
                                   int commitBatchSize, long snapshotBlockHeight) {
        this(dataTransferManagement, snapshotBlockHeight);
        this.commitBatchSize = commitBatchSize;
    }

    public DeleteCopiedDataCommand(DataTransferManagementReceiver dataTransferManagement,
                                   long snapshotBlockHeight) {
        this.dataTransferManagement = Objects.requireNonNull(dataTransferManagement, "dataTransferManagement is NULL");
        this.snapshotBlockHeight = snapshotBlockHeight;
        tableNameList.add(BLOCK_TABLE_NAME);
    }

    public DeleteCopiedDataCommand(
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
        log.debug("Delete Block/Transaction Data from main DB Command execute...");
        CommandParamInfo paramInfo = new CommandParamInfoImpl(
                this.tableNameList, this.commitBatchSize, this.snapshotBlockHeight, null);
        return dataTransferManagement.deleteCopiedData(paramInfo);
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
