/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.commands;

import java.util.List;
import java.util.Set;

/**
 * Interface command parameters information.
 *
 * @author yuriy.larin
 */
public interface CommandParamInfo {

    List<String> getTableNameList();

    void setTableNameList(List<String> tableNameList);

    int getCommitBatchSize();

    void setCommitBatchSize(int commitBatchSize);

    Long getSnapshotBlockHeight();

    void setSnapshotBlockHeight(Long snapshotBlockHeight);

    byte[] getShardHash();

    void setShardHash(byte[] shardHash);

    Set<Long> getDbIdExclusionSet();

    void setDbIdExclusionSet(Set<Long> dbIdExclusionSet);

}
