/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.shard.commands;

import java.util.Collections;
import java.util.List;

/**
 * {@inheritDoc}
 */
public class CommandParamInfoImpl implements CommandParamInfo {

    private List<String> tableNameList = Collections.emptyList(); // processed tables list
    private int commitBatchSize = -1;
    private Long snapshotBlockHeight = -1L;
    private byte[] shardHash;

    public CommandParamInfoImpl() {
    }

    public CommandParamInfoImpl(List<String> tableNameList, int commitBatchSize, Long snapshotBlockHeight) {
        this.shardHash = new byte[0];
        this.tableNameList = tableNameList;
        this.commitBatchSize = commitBatchSize;
        this.snapshotBlockHeight = snapshotBlockHeight;
    }

    public CommandParamInfoImpl(List<String> tableNameList, int commitBatchSize, Long snapshotBlockHeight, byte[] shardHash) {
        this(tableNameList, commitBatchSize, snapshotBlockHeight);
        this.shardHash = shardHash;
    }

    public CommandParamInfoImpl(byte[] shardHash) {
        this.shardHash = shardHash;
    }

    public List<String> getTableNameList() {
        return tableNameList;
    }

    public void setTableNameList(List<String> tableNameList) {
        this.tableNameList = tableNameList;
    }

    public int getCommitBatchSize() {
        return commitBatchSize;
    }

    public void setCommitBatchSize(int commitBatchSize) {
        this.commitBatchSize = commitBatchSize;
    }

    public Long getSnapshotBlockHeight() {
        return snapshotBlockHeight;
    }

    public void setSnapshotBlockHeight(Long snapshotBlockHeight) {
        this.snapshotBlockHeight = snapshotBlockHeight;
    }

    public byte[] getShardHash() {
        return shardHash;
    }

    public void setShardHash(byte[] shardHash) {
        this.shardHash = shardHash;
    }
}
