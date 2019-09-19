/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.cache;

import com.apollocurrency.aplwallet.apl.util.cache.CacheConfigurator;

public class PublicKeyCacheConfig extends CacheConfigurator {

    public static final String PUBLIC_KEY_CACHE_NAME = "PUBLIC_KEY_CACHE";

    public PublicKeyCacheConfig(int percentCapacity) {
        super(PUBLIC_KEY_CACHE_NAME, 32, percentCapacity);
        cacheBuilder().initialCapacity(16);
    }
}
