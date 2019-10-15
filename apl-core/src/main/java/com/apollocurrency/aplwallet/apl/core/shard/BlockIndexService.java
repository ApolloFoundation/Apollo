/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.db.dao.model.BlockIndex;

import java.util.List;

public interface BlockIndexService {
    BlockIndex getByBlockId(long blockId);

    Long getShardIdByBlockId(long blockId);

    BlockIndex getByBlockHeight(int blockHeight);

    Long getShardIdByBlockHeight(int blockHeight);

    Integer getHeight(long id);

    List<Long> getBlockIdsAfter(int height, int limit);

    int hardDeleteAllBlockIndex();
}
