package com.apollocurrency.aplwallet.apl.core.shard;

import java.util.List;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;

/**
 * {@inheritDoc}
 */
public class DatabaseMetaInfoImpl implements DatabaseMetaInfo {

    private TransactionalDataSource dataSource; // source or target
    private String newFileName;
//    private List<String> statementList; // processed tables list
    private int commitBatchSize;
    private Block snapshotBlock;
    private MigrateState migrateState;
    private Long snapshotBlockHeight;

    public DatabaseMetaInfoImpl(TransactionalDataSource dataSource,
                                String newFileName, /*List<String> statementList,*/
                                int commitBatchSize, MigrateState migrateState,
                                Block snapshotBlock,
                                Long snapshotBlockHeight) {
        this.dataSource = dataSource;
        this.newFileName = newFileName;
//        this.statementList = statementList;
        this.commitBatchSize = commitBatchSize;
        this.migrateState = migrateState;
        this.snapshotBlock = snapshotBlock;
        this.snapshotBlockHeight = snapshotBlockHeight;
    }

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

/*
    public List<String> getStatementList() {
        return statementList;
    }

    public void setStatementList(List<String> statementList) {
        this.statementList = statementList;
    }
*/

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

    public Block getSnapshotBlock() {
        return snapshotBlock;
    }

    public void setSnapshotBlock(Block snapshotBlock) {
        this.snapshotBlock = snapshotBlock;
    }

    public Long getSnapshotBlockHeight() {
        return snapshotBlockHeight;
    }

    public void setSnapshotBlockHeight(Long snapshotBlockHeight) {
        this.snapshotBlockHeight = snapshotBlockHeight;
    }
}
