/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.shard;

import static com.apollocurrency.aplwallet.apl.core.cache.BlockIndexCacheConfig.BLOCK_INDEX_CACHE_NAME;
import com.apollocurrency.aplwallet.apl.core.db.dao.BlockIndexDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.BlockIndex;
import com.apollocurrency.aplwallet.apl.util.cache.CacheProducer;
import com.apollocurrency.aplwallet.apl.util.cache.CacheType;
import com.google.common.cache.Cache;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Block finding service that uses block_index table via BlockIndexDao This
 * class is wrapper of DAO with managed cache
 * TODO: profile this class and optimize caching
 * @author alukin@gmail.com
 */
@Singleton
public class BlockIndexService {

    private final BlockIndexDao blockIndexDao;
    
    
    private final Cache<Long, BlockIndex> blockIndexCache;

    @Inject
    public BlockIndexService(BlockIndexDao blockIndexDao,
            @CacheProducer
            @CacheType(BLOCK_INDEX_CACHE_NAME)                    
            Cache<Long, BlockIndex> blockIndexCache
    ) {
        this.blockIndexDao = blockIndexDao;
        this.blockIndexCache = blockIndexCache;
    }

    public BlockIndex getByBlockId(long blockId) {
        BlockIndex res=null;
        if(blockIndexCache!=null){
            res = blockIndexCache.getIfPresent(blockId);
        }
        if(res==null){
            res = blockIndexDao.getByBlockId(blockId);
            if(blockIndexCache!=null && res!=null){
                blockIndexCache.put(blockId, res);
            }
        }
        return res;
    }

    public Long getShardIdByBlockId(long blockId) {
        Long res = blockIndexDao.getShardIdByBlockId(blockId);
        return res;
    }

    public BlockIndex getByBlockHeight(int blockHeight) {
        BlockIndex res = blockIndexDao.getByBlockHeight(blockHeight);
        return res;
    }

    public Long getShardIdByBlockHeight(int blockHeight) {
        Long res = blockIndexDao.getShardIdByBlockHeight(blockHeight);
        return res;
    }

    public Integer getHeight(long id) {
        Integer res = blockIndexDao.getHeight(id);
        return res;
    }

    public List<BlockIndex> getAllBlockIndex() {
        return blockIndexDao.getAllBlockIndex();
    }

    public BlockIndex getLast() {
        BlockIndex res = blockIndexDao.getLast();
        return res;
    }

    public Integer getLastHeight() {
        return blockIndexDao.getLastHeight();
    }

    public List<Long> getBlockIdsAfter(int height, int limit) {
        return blockIndexDao.getBlockIdsAfter(height, limit);
    }

    public int count() {
        return blockIndexDao.count();
    }

    public long countBlockIndexByShard(long shardId) {
        return blockIndexDao.countBlockIndexByShard(shardId);
    }

    public int saveBlockIndex(BlockIndex blockIndex) {
        return blockIndexDao.saveBlockIndex(blockIndex);
    }

    public int updateBlockIndex(BlockIndex blockIndex) {
        return blockIndexDao.updateBlockIndex(blockIndex);
    }

    public int hardBlockIndex(BlockIndex blockIndex) {
       return blockIndexDao.hardBlockIndex(blockIndex);
    }

    public int hardDeleteAllBlockIndex() {
        return blockIndexDao.hardDeleteAllBlockIndex();
    }
  

}
