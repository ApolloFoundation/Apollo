/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.shard.helper;

import com.apollocurrency.aplwallet.apl.core.shard.ExcludeInfo;

/**
 * Composes several parameters used by different operations.
 *
 * @author yuriy.larin
 */
public class TableOperationParams {

    String tableName;
    long batchCommitSize;
    Integer snapshotBlockHeight;
    Long shardId;
    ExcludeInfo excludeInfo;

    public TableOperationParams(String tableName, long batchCommitSize,
                                Integer snapshotBlockHeight, Long shardId,
                                ExcludeInfo excludeInfo) {
        this.tableName = tableName;
        this.batchCommitSize = batchCommitSize;
        this.snapshotBlockHeight = snapshotBlockHeight;
        this.shardId = shardId;
        this.excludeInfo = excludeInfo;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("TableOperationParams{");
        sb.append("tableName='").append(tableName).append('\'');
        sb.append(", batchCommitSize=").append(batchCommitSize);
        sb.append(", snapshotBlockHeight=").append(snapshotBlockHeight);
        sb.append(", shardId=").append(shardId);
        sb.append('}');
        return sb.toString();
    }
}
