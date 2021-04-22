/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.cache;

import com.apollocurrency.aplwallet.apl.util.cache.CacheConfigurator;

import java.util.concurrent.TimeUnit;

public class RemovedTxsCacheConfig extends CacheConfigurator {

    public static final String CACHE_NAME = "REMOVED_TXS_CACHE";

    public RemovedTxsCacheConfig(int priority) {
        super(CACHE_NAME,
            8,
            priority);

        cacheBuilder().expireAfterWrite(10, TimeUnit.MINUTES);
    }
}
