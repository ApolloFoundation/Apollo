/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.cache;

import com.apollocurrency.aplwallet.apl.util.cache.CacheConfigurator;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;

import static com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager.MemoryUsageCalculator.INT_SIZE;
import static com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager.MemoryUsageCalculator.LONG_SIZE;

public class BlockIndexCacheConfig extends CacheConfigurator {

    public static final String BLOCK_INDEX_CACHE_NAME = "BLOCK_INDEX_CACHE";

    public BlockIndexCacheConfig(int priority) {
        super(BLOCK_INDEX_CACHE_NAME, InMemoryCacheManager.newCalc()
                .addAggregation(LONG_SIZE)
                .addAggregation(INT_SIZE)
                .calc(),
                priority);
    }
}
