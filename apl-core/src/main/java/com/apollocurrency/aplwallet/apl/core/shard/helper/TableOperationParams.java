/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.shard.helper;

import java.util.Optional;
import java.util.Set;

/**
 * Composes several parameters used by different operations.
 *
 * @author yuriy.larin
 */
public class TableOperationParams {

    String tableName;
    long batchCommitSize;
    Long snapshotBlockHeight;
    Optional<Long> shardId;
    Optional<Set<Long>> dbIdsExclusionSet;

    public TableOperationParams(String tableName, long batchCommitSize, Long snapshotBlockHeight, Optional<Long> shardId, Optional<Set<Long>> dbIdsExclusionSet) {
        this.tableName = tableName;
        this.batchCommitSize = batchCommitSize;
        this.snapshotBlockHeight = snapshotBlockHeight;
        this.shardId = shardId;
        this.dbIdsExclusionSet = dbIdsExclusionSet;
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
