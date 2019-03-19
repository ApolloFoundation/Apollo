/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.commands;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.shard.DataTransferManagementReceiver;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import org.slf4j.Logger;

/**
 * Update records in specified tables so they point to snapshot block in main db
 */
public class ReLinkDataCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(ReLinkDataCommand.class);

    private DataTransferManagementReceiver dataTransferManagement;
    private List<String> tableNameList = new ArrayList<>();
    private int commitBatchSize = DEFAULT_COMMIT_BATCH_SIZE;
    private long snapshotBlockHeight = 0L;

    public ReLinkDataCommand(DataTransferManagementReceiver dataTransferManagement,
                             int commitBatchSize, long snapshotBlockHeight) {
        this(dataTransferManagement, snapshotBlockHeight);
        this.commitBatchSize = commitBatchSize;
    }

    public ReLinkDataCommand(DataTransferManagementReceiver dataTransferManagement,
                             long snapshotBlockHeight) {
        this.dataTransferManagement = Objects.requireNonNull(dataTransferManagement, "dataTransferManagement is NULL");
        this.snapshotBlockHeight = snapshotBlockHeight;
        tableNameList.add(GENESIS_PUBLIC_KEY_TABLE_NAME);
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
        log.debug("Update Linked Data Command execute...");
        CommandParamInfo paramInfo = new CommandParamInfoImpl(
                this.tableNameList, this.commitBatchSize, this.snapshotBlockHeight);
        return dataTransferManagement.relinkDataToSnapshotBlock(paramInfo);
    }
}
