package com.apollocurrency.aplwallet.apl.core.cache;

import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.util.cache.CacheConfigurator;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;
import com.google.common.cache.CacheLoader;

import java.util.concurrent.TimeUnit;

public class DexOrderFreezingCacheConfig extends CacheConfigurator {

        public static final String CACHE_NAME = "DEX_ORDER_FREEZING_CACHE";

        public DexOrderFreezingCacheConfig(int priority, DexService dexService) {
            super(CACHE_NAME,
                    InMemoryCacheManager.newCalc()
                            .addLongPrimitive() // orderId
                            .addBooleanPrimitive() // hasFrozenMoney
                            .calc(),
                    priority, CacheLoader.from((key)-> {
                        long id = ((Long) key);
                        DexOrder order = dexService.getOrder(id);
                        if (order == null) {
                            throw new RuntimeException("Order does not exist " + id);
                        }
                        return dexService.hasFrozenMoney(order);
                    }));

            cacheBuilder().expireAfterWrite(1, TimeUnit.MINUTES);
        }
}
