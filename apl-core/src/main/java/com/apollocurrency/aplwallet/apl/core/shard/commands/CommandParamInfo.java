/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.shard.commands;

import static com.apollocurrency.aplwallet.apl.core.shard.ShardConstants.DEFAULT_COMMIT_BATCH_SIZE;

import com.apollocurrency.aplwallet.apl.core.shard.ExcludeInfo;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * {@inheritDoc}
 */
@Builder
@Getter
public class CommandParamInfo {

    private List<String> tableNameList = Collections.emptyList(); // processed tables list
    private int commitBatchSize = DEFAULT_COMMIT_BATCH_SIZE;
    private Integer snapshotBlockHeight = -1;
    private byte[] shardHash; // either 'merkle tree hash' or 'zip CRC'
    private ExcludeInfo excludeInfo; // 'phased transaction' db_id to be excluded from all processing (no copy, delete, export)
    private boolean isZipCrcStored = false; // either ZIP or merkle tree hash
    private Long[] generatorIds; // 3 generator ids before snapshot block with height offset (-1, -2, -3)
    private Long shardId; // id of shard to create
    private int blockchainHeight; //height of blockchain when sharding was triggered
}
