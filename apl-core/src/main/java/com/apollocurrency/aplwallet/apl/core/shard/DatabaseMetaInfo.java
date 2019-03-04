package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;

/**
 * Interface + Implementation for creating SOURCE and TARGET database data source instances
 */
public interface DatabaseMetaInfo {

    TransactionalDataSource getDataSource();

    void setDataSource(TransactionalDataSource dataSource);

    String getNewFileName();

    void setNewFileName(String newFileName);

/*
    List<String> getStatementList();

    void setStatementList(List<String> statementList);
*/

    int getCommitBatchSize();

    void setCommitBatchSize(int commitBatchSize);

    MigrateState getMigrateState();

    void setMigrateState(MigrateState migrateState);

    Block getSnapshotBlock();

    void setSnapshotBlock(Block snapshotBlock);
}
