/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.commands;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.core.shard.ShardEngine;
import org.slf4j.Logger;

/**
 * Export specified tables + 'derived tables' into CSV.
 */
public class CsvExportCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(CsvExportCommand.class);

    private ShardEngine shardEngine;
    private List<String> tableNameList;
    private int commitBatchSize;
    private int snapshotBlockHeight;
    private Set<Long> dbIdsExclusionList;

    public CsvExportCommand(ShardEngine shardEngine,
                            int commitBatchSize, int snapshotBlockHeight, List<String> tableNameList, Set<Long> dbIdsExclusionList) {
        this.shardEngine = Objects.requireNonNull(shardEngine, "shardEngine is NULL");
        this.snapshotBlockHeight = snapshotBlockHeight;
        this.commitBatchSize = commitBatchSize <= 0 ? DEFAULT_COMMIT_BATCH_SIZE : commitBatchSize;
        this.dbIdsExclusionList = dbIdsExclusionList == null ? Collections.emptySet() : dbIdsExclusionList;
        this.tableNameList = tableNameList == null ? new ArrayList<>() : tableNameList;
    }

    public CsvExportCommand(ShardEngine shardEngine,
                            int snapshotBlockHeight, Set<Long> dbIdsExclusionList) {
        this(shardEngine,  null, DEFAULT_COMMIT_BATCH_SIZE, snapshotBlockHeight, dbIdsExclusionList);
        // tables to be exported together with 'derived tables'
        tableNameList = List.of(BLOCK_INDEX_TABLE_NAME, TRANSACTION_SHARD_INDEX_TABLE_NAME, SHARD_TABLE_NAME);
    }

    public CsvExportCommand(
            ShardEngine shardEngine,
            List<String> tableNameList,
            int commitBatchSize,
            int snapshotBlockHeight, Set<Long> dbIdsExclusionList) {
        this(shardEngine, commitBatchSize, snapshotBlockHeight, tableNameList, dbIdsExclusionList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState execute() {
        log.debug("CSV Export Command execute...");
        CommandParamInfo paramInfo = new CommandParamInfoImpl(
                this.tableNameList, this.commitBatchSize, this.snapshotBlockHeight, dbIdsExclusionList);
        return shardEngine.exportCsv(paramInfo);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("CsvExportCommand{");
        sb.append("tableNameList=").append(tableNameList);
        sb.append(", commitBatchSize=").append(commitBatchSize);
        sb.append(", snapshotBlockHeight=").append(snapshotBlockHeight);
        sb.append('}');
        return sb.toString();
    }
}
