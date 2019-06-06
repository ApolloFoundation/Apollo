/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.commands;

import java.util.List;
import java.util.Set;

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

    Set<Long> getDbIdExclusionSet();

    void setDbIdExclusionSet(Set<Long> dbIdExclusionSet);

    /**
     * To check if getShardHash() returns 'merkle tree hash' or 'zip crc hash'
     * @return true when 'zip crc'
     */
    boolean isZipCrcStored();

}
