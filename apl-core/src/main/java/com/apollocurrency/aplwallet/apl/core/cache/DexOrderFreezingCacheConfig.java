package com.apollocurrency.aplwallet.apl.core.cache;

import com.apollocurrency.aplwallet.apl.exchange.model.OrderFreezing;
import com.apollocurrency.aplwallet.apl.util.cache.CacheConfigurator;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;
import com.google.common.cache.CacheLoader;

import java.util.concurrent.TimeUnit;

public class DexOrderFreezingCacheConfig extends CacheConfigurator {

    public static final String CACHE_NAME = "DEX_ORDER_FREEZING_CACHE";

    public DexOrderFreezingCacheConfig(int priority, CacheLoader<Long, OrderFreezing> loader) {
        super(CACHE_NAME,
            InMemoryCacheManager.newCalc()
                .addLongPrimitive() // orderId
                .addBooleanPrimitive() // hasFrozenMoney
                .calc(),
            priority, loader);

        cacheBuilder().expireAfterWrite(1, TimeUnit.MINUTES);
    }
}
