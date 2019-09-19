/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.cache;

import com.apollocurrency.aplwallet.apl.util.cache.CacheConfiguration;
import com.apollocurrency.aplwallet.apl.util.cache.CacheConfigurator;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheConfigurator;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;

@Singleton
@Slf4j
public class AplCacheConfig implements InMemoryCacheConfigurator {

    private static final int ADDRESSABLE_FOR_CACHE = 60; //60 percent of Available memory;

    private CacheConfiguration[] cacheConfigurations = {
            new PrimaryDbKeyCacheConfig(50),
            new PublicKeyCacheConfig(50)
    };

    @PostConstruct
    public void setUp(){
        log.debug("Runtime: maMemory={} totalMemory={}", Runtime.getRuntime().maxMemory(), Runtime.getRuntime().totalMemory());
        log.debug("Available for cache={}", getAvailableMemory());
        int sum = Arrays.stream(cacheConfigurations).mapToInt(CacheConfiguration::getCacheCapacity).sum();
        if (sum>100){
            RuntimeException exception = new IllegalArgumentException("Summary capacity exceeds 100 percent.");
            log.error(exception.getMessage());
            throw exception;
        }
    }

    @Override
    public long getAvailableMemory() {
        long mem = Math.min(Runtime.getRuntime().maxMemory(), Runtime.getRuntime().totalMemory())/100;

        mem = mem*ADDRESSABLE_FOR_CACHE;

        return mem;
    }

    @Override
    public List<CacheConfiguration> getConfiguredCaches() {
        return List.of(cacheConfigurations);
    }
}
