/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.shard.commands;

import static com.apollocurrency.aplwallet.apl.core.shard.ShardConstants.DEFAULT_COMMIT_BATCH_SIZE;

import com.apollocurrency.aplwallet.apl.core.shard.ExcludeInfo;

import java.util.Collections;
import java.util.List;

/**
 * {@inheritDoc}
 */
public class CommandParamInfoImpl implements CommandParamInfo {

    private List<String> tableNameList = Collections.emptyList(); // processed tables list
    private int commitBatchSize = DEFAULT_COMMIT_BATCH_SIZE;
    private Integer snapshotBlockHeight = -1;
    private byte[] shardHash; // either 'merkle tree hash' or 'zip CRC'
    private ExcludeInfo excludeInfo; // 'phased transaction' db_id to be excluded from all processing (no copy, delete, export)
    private boolean isZipCrcStored = false; // either ZIP or merkle tree hash
    private Long[] generatorIds; // 3 generator ids before snapshot block with height offset (-1, -2, -3)

    public CommandParamInfoImpl() {
    }

    public CommandParamInfoImpl(List<String> tableNameList, int commitBatchSize, Integer snapshotBlockHeight, ExcludeInfo excludeInfos) {
        this.shardHash = new byte[0];
        this.tableNameList = tableNameList;
        this.commitBatchSize = commitBatchSize;
        this.snapshotBlockHeight = snapshotBlockHeight;
        this.excludeInfo = excludeInfos;
    }

    public CommandParamInfoImpl(List<String> tableNameList, int commitBatchSize, Integer snapshotBlockHeight, byte[] shardHash, ExcludeInfo excludeInfos) {
        this(tableNameList, commitBatchSize, snapshotBlockHeight, excludeInfos);
        this.shardHash = shardHash;
    }

    public CommandParamInfoImpl(Long[] generatorIds) {
        this.generatorIds = generatorIds;
    }

    @Override
    public ExcludeInfo getExcludeInfo() {
        return excludeInfo;
    }

    @Override
    public void setExcludeInfo(ExcludeInfo excludeInfo) {
        this.excludeInfo = excludeInfo;
    }

    public CommandParamInfoImpl(byte[] shardHash) {
        this.shardHash = shardHash;
    }

    public CommandParamInfoImpl(byte[] shardHash, boolean isZipCrcHash) {
        this.isZipCrcStored = isZipCrcHash;
        if (this.isZipCrcStored) {
            this.shardHash = shardHash;
        }
    }

    public boolean isZipCrcStored() {
        return isZipCrcStored;
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

    public Integer getSnapshotBlockHeight() {
        return snapshotBlockHeight;
    }

    public void setSnapshotBlockHeight(Integer snapshotBlockHeight) {
        this.snapshotBlockHeight = snapshotBlockHeight;
    }

    public byte[] getShardHash() {
        return shardHash;
    }

    @Override
    public Long[] getGeneratorIds() {
        return generatorIds;
    }

    public void setShardHash(byte[] shardHash) {
        this.shardHash = shardHash;
    }
}
