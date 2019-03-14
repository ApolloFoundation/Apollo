/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.shard.helper;

import java.util.Optional;

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

    public TableOperationParams(String tableName, long batchCommitSize, Long snapshotBlockHeight, Optional<Long> shardId) {
        this.tableName = tableName;
        this.batchCommitSize = batchCommitSize;
        this.snapshotBlockHeight = snapshotBlockHeight;
        this.shardId = shardId;
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
