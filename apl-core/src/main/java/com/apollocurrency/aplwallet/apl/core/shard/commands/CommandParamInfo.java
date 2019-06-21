/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.commands;

import com.apollocurrency.aplwallet.apl.core.shard.ExcludeInfo;

import java.util.List;

/**
 * Interface command parameters information. It keeps the info passed to steps.
 *
 * @author yuriy.larin
 */
public interface CommandParamInfo {

    List<String> getTableNameList();

    void setTableNameList(List<String> tableNameList);

    int getCommitBatchSize();

    void setCommitBatchSize(int commitBatchSize);

    Integer getSnapshotBlockHeight();

    void setSnapshotBlockHeight(Integer snapshotBlockHeight);

    byte[] getShardHash();

    void setShardHash(byte[] shardHash);

    ExcludeInfo getExcludeInfo();

    void setExcludeInfo(ExcludeInfo excludeInfo);

    /**
     * To check if getShardHash() returns 'merkle tree hash' or 'zip crc hash'
     * @return true when 'zip crc'
     */
    boolean isZipCrcStored();

    default Long[] getGeneratorIds() {
        return null;
    }
}
