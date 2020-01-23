/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.cache;

import com.apollocurrency.aplwallet.apl.util.cache.CacheConfigurator;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;

import static com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager.MemoryUsageCalculator.LONG_SIZE;

public class PublicKeyCacheConfig extends CacheConfigurator {

    public static final String PUBLIC_KEY_CACHE_NAME = "PUBLIC_KEY_CACHE";

    public PublicKeyCacheConfig(int priority) {
        super(PUBLIC_KEY_CACHE_NAME,
                InMemoryCacheManager.newCalc()
                        .addLongPrimitive() // accountId
                        .addArrayExtra(32) //publickey byte array
                        .addBooleanPrimitive() //latest
                        .addLongPrimitive() //dbId
                        .addInt() //height
                        .addAggregation(LONG_SIZE) //dbKey object
                        .calc(),
                priority);

        cacheBuilder().initialCapacity(16);
    }
}
