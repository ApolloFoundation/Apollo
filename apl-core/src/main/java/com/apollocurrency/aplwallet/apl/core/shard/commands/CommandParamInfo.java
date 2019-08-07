/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.shard.commands;

import com.apollocurrency.aplwallet.apl.core.shard.model.ExcludeInfo;
import com.apollocurrency.aplwallet.apl.core.shard.model.PrevBlockData;
import com.apollocurrency.aplwallet.apl.core.shard.model.TableInfo;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * {@inheritDoc}
 */
@Builder
@Getter
public class CommandParamInfo {

    private List<TableInfo> tableInfoList; // processed tables list
    private int commitBatchSize;
    private Integer snapshotBlockHeight;
    private byte[] shardHash; // 'merkle tree hash'
    private ExcludeInfo excludeInfo; // 'phased transaction' db_id to be excluded from all processing (no copy, delete, export)
    private Long shardId; // id of shard to create
    private PrevBlockData prevBlockData; // required previous block information for consensus
}
