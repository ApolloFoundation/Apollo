package com.apollocurrency.aplwallet.apl.core.shard;

import java.sql.Statement;
import java.util.List;

import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;

/**
 * Interface + Implementation for creating SOURCE and TARGET database data source instances
 */
public interface DatabaseMetaInfo {

    TransactionalDataSource getDataSource();

    void setDataSource(TransactionalDataSource dataSource);

    String getNewFileName();

    void setNewFileName(String newFileName);

    List<Statement> getStatementList();

    void setStatementList(List<Statement> statementList);

    int getCommitBatchSize();

    void setCommitBatchSize(int commitBatchSize);

    MigrateState getMigrateState();

    void setMigrateState(MigrateState migrateState);

}
