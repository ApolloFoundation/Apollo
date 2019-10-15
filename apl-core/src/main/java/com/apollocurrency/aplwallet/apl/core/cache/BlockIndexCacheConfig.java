/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.cache;

import com.apollocurrency.aplwallet.apl.util.cache.CacheConfigurator;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;

public class BlockIndexCacheConfig extends CacheConfigurator {

    public static final String BLOCK_INDEX_CACHE_NAME = "BLOCK_INDEX_CACHE";

    public BlockIndexCacheConfig(int priority) {
        super(BLOCK_INDEX_CACHE_NAME, InMemoryCacheManager.newCalc()
                        .addLongPrimitive()
                        .addInt()
                        .calc(),
                priority);
    }
}
