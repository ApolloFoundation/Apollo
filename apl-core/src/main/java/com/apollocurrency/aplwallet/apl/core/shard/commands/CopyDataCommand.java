/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.commands;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.shard.DataTransferManagementReceiver;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import org.slf4j.Logger;

/**
 * Command copy block + transaction data from main into shard database.
 *
 * @author yuriy.larin
 */
public class CopyDataCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(CopyDataCommand.class);

    private DataTransferManagementReceiver dataTransferManagement;
    private List<String> tableNameList = new LinkedList<>();
    private int commitBatchSize = 100;
    private long snapshotBlockHeight = 0L;

    public CopyDataCommand(DataTransferManagementReceiver dataTransferManagement,
                           int commitBatchSize, long snapshotBlockHeight) {
        this(dataTransferManagement, snapshotBlockHeight);
        this.commitBatchSize = commitBatchSize;
    }

    public CopyDataCommand(DataTransferManagementReceiver dataTransferManagement,
                           long snapshotBlockHeight) {
        this.dataTransferManagement = Objects.requireNonNull(dataTransferManagement, "dataTransferManagement is NULL");
        this.snapshotBlockHeight = snapshotBlockHeight;
        tableNameList.add("BLOCK");
        tableNameList.add("TRANSACTION");
    }

    public CopyDataCommand(
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
        log.debug("Copy Shard Data Command execute...");
        CommandParamInfo paramInfo = new CommandParamInfoImpl(
                this.tableNameList, this.commitBatchSize, this.snapshotBlockHeight);
        return dataTransferManagement.copyDataToShard(paramInfo);
    }
}
