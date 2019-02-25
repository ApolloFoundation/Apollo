package com.apollocurrency.aplwallet.apl.core.shard;

import javax.inject.Singleton;
import java.sql.Statement;
import java.util.List;

import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;

/**
 * {@inheritDoc}
 */
@Singleton
public class DatabaseMetaInfoImpl implements DatabaseMetaInfo {

    private TransactionalDataSource dataSource; // source or target
    private String newFileName;
    private List<Statement> statementList;
    private int commitBatchSize;
    private MigrateState migrateState;

    public TransactionalDataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(TransactionalDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String getNewFileName() {
        return newFileName;
    }

    public void setNewFileName(String newFileName) {
        this.newFileName = newFileName;
    }

    public List<Statement> getStatementList() {
        return statementList;
    }

    public void setStatementList(List<Statement> statementList) {
        this.statementList = statementList;
    }

    public int getCommitBatchSize() {
        return commitBatchSize;
    }

    public void setCommitBatchSize(int commitBatchSize) {
        this.commitBatchSize = commitBatchSize;
    }

    public MigrateState getMigrateState() {
        return migrateState;
    }

    public void setMigrateState(MigrateState migrateState) {
        this.migrateState = migrateState;
    }
}
