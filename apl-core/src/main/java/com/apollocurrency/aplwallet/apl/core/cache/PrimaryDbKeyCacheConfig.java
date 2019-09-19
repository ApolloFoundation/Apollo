/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.cache;

import com.apollocurrency.aplwallet.apl.util.cache.CacheConfigurator;

public class PrimaryDbKeyCacheConfig extends CacheConfigurator {

    public static final String PRIMARY_DB_KEY_CACHE_NAME = "PRIMARY_DB_KEY_CACHE";

    public PrimaryDbKeyCacheConfig(int percentCapacity) {
        super(PRIMARY_DB_KEY_CACHE_NAME, 8, percentCapacity);
        cacheBuilder().initialCapacity(1024);
    }

}
