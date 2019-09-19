/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.cache;

import com.apollocurrency.aplwallet.apl.util.cache.CacheConfigurator;

import java.util.concurrent.TimeUnit;

public class ShardDsCacheConfigurator extends CacheConfigurator {

    public static final String SHARD_DS_CACHE_NAME = "SHARD_DS_CACHE";

    public ShardDsCacheConfigurator(int percentCapacity) {
        super(SHARD_DS_CACHE_NAME, 8, percentCapacity);
        cacheBuilder().expireAfterAccess(60, TimeUnit.MINUTES);
    }
}
