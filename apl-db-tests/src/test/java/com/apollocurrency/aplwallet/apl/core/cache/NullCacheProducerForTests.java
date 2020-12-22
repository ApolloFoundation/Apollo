/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.cache;

import com.apollocurrency.aplwallet.apl.util.cache.CacheProducer;
import com.google.common.cache.Cache;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Singleton;

@Singleton
public class NullCacheProducerForTests {
    @Produces
    @CacheProducer
    public <K, V> Cache<K, V> acquireCache(InjectionPoint injectionPoint) {
        return null;
    }
}
