/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.dao.appdata.BlockIndexDao;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.BlockIndex;
import com.apollocurrency.aplwallet.apl.util.cache.CacheProducer;
import com.apollocurrency.aplwallet.apl.util.cache.CacheType;
import com.google.common.cache.Cache;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.core.cache.BlockIndexCacheConfig.BLOCK_INDEX_CACHE_NAME;

/**
 * Block finding service that uses block_index table via BlockIndexDao This
 * class is wrapper of DAO with managed cache
 *
 * @author alukin@gmail.com
 */
@Singleton
public class BlockIndexServiceImpl implements BlockIndexService {

    private final BlockIndexDao blockIndexDao;
    private final Cache<Long, BlockIndex> blockIndexCache;

    @Inject
    public BlockIndexServiceImpl(BlockIndexDao blockIndexDao,
                                 @CacheProducer
                                 @CacheType(BLOCK_INDEX_CACHE_NAME)
                                     Cache<Long, BlockIndex> blockIndexCache
    ) {
        this.blockIndexDao = blockIndexDao;
        this.blockIndexCache = blockIndexCache;
    }

    @Override
    public BlockIndex getByBlockId(long blockId) {
        BlockIndex res = null;
        if (blockIndexCache != null) {
            res = blockIndexCache.getIfPresent(blockId);
        }
        if (res == null) {
            res = blockIndexDao.getByBlockId(blockId);
            if (blockIndexCache != null && res != null) {
                blockIndexCache.put(blockId, res);
            }
        }
        return res;
    }

    @Override
    public Long getShardIdByBlockId(long blockId) {
        Long res = blockIndexDao.getShardIdByBlockId(blockId);
        return res;
    }

    @Override
    public BlockIndex getByBlockHeight(int blockHeight) {
        BlockIndex res = blockIndexDao.getByBlockHeight(blockHeight);
        return res;
    }

    @Override
    public Long getShardIdByBlockHeight(int blockHeight) {
        Long res = blockIndexDao.getShardIdByBlockHeight(blockHeight);
        return res;
    }

    @Override
    public Integer getHeight(long id) {
        BlockIndex blockIndex = getByBlockId(id);
        if (blockIndex != null) {
            return blockIndex.getBlockHeight();
        }
        return null;
    }

    @Override
    public List<Long> getBlockIdsAfter(int height, int limit) {
        return blockIndexDao.getBlockIdsAfter(height, limit);
    }

    @Override
    public int hardDeleteAllBlockIndex() {
        if (blockIndexCache != null) {
            blockIndexCache.cleanUp();
        }
        return blockIndexDao.hardDeleteAllBlockIndex();
    }
}
