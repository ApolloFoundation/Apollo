/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.commands;

import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.core.shard.ShardConstants;
import com.apollocurrency.aplwallet.apl.core.shard.ShardEngine;
import com.apollocurrency.aplwallet.apl.core.shard.model.ExcludeInfo;
import com.apollocurrency.aplwallet.apl.core.shard.model.TableInfo;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Export specified tables + 'derived tables' into CSV.
 */
public class CsvExportCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(CsvExportCommand.class);

    private ShardEngine shardEngine;
    private List<TableInfo> tableInfoList;
    private int commitBatchSize;
    private int snapshotBlockHeight;
    private ExcludeInfo excludeInfo;

    public CsvExportCommand(ShardEngine shardEngine,
                            int commitBatchSize, int snapshotBlockHeight, List<TableInfo> tableInfoList, ExcludeInfo excludeInfo) {
        this.shardEngine = Objects.requireNonNull(shardEngine, "shardEngine is NULL");
        this.snapshotBlockHeight = snapshotBlockHeight;
        this.commitBatchSize = commitBatchSize <= 0 ? ShardConstants.DEFAULT_COMMIT_BATCH_SIZE : commitBatchSize;
        this.excludeInfo = excludeInfo;
        this.tableInfoList = tableInfoList == null ? new ArrayList<>() : tableInfoList;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState execute() {
        log.debug("CSV Export Command execute...");
        CommandParamInfo paramInfo = CommandParamInfo.builder()
            .tableInfoList(this.tableInfoList)
            .commitBatchSize(this.commitBatchSize)
            .snapshotBlockHeight(this.snapshotBlockHeight)
            .excludeInfo(this.excludeInfo)
            .build();
        return shardEngine.exportCsv(paramInfo);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("CsvExportCommand{");
        sb.append("tableInfoList=").append(tableInfoList);
        sb.append(", commitBatchSize=").append(commitBatchSize);
        sb.append(", snapshotBlockHeight=").append(snapshotBlockHeight);
        sb.append('}');
        return sb.toString();
    }
}
