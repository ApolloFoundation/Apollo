package com.apollocurrency.aplwallet.apl.core.shard.helper;

import java.sql.Connection;

public interface BatchedSelectInsert {

    void reset();

    long generateInsertStatementsWithPaging(
            Connection sourceConnect, Connection targetConnect, String tableName,
            long batchCommitSize, Long snapshotBlockHeight) throws Exception;
}
