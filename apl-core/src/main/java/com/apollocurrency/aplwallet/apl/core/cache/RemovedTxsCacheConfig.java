/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.cache;

import com.apollocurrency.aplwallet.apl.util.cache.CacheConfigurator;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;

import java.util.concurrent.TimeUnit;

public class RemovedTxsCacheConfig extends CacheConfigurator {

    public static final String CACHE_NAME = "REMOVED_TXS_CACHE";

    public RemovedTxsCacheConfig(int priority) {
        super(CACHE_NAME,
            InMemoryCacheManager.newCalc()
                .addLongPrimitive()
                .addLongPrimitive()
            .calc(),
            priority);

        cacheBuilder().expireAfterWrite(10, TimeUnit.MINUTES);
    }
}
