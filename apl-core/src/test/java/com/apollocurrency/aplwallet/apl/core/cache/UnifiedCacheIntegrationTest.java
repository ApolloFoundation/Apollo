/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.cache;

import com.apollocurrency.aplwallet.apl.exchange.model.OrderFreezing;
import com.apollocurrency.aplwallet.apl.util.cache.CacheProducer;
import com.apollocurrency.aplwallet.apl.util.cache.CacheType;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheLoader;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static com.apollocurrency.aplwallet.apl.core.cache.PublicKeyCacheConfig.PUBLIC_KEY_CACHE_NAME;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

@EnableWeld
public class UnifiedCacheIntegrationTest {

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(AplCacheConfig.class, InMemoryCDICacheFactory.class, CacheLoaderProducer.class).build();

    @Inject
    @CacheProducer
    @CacheType(PUBLIC_KEY_CACHE_NAME)
    Cache<String, byte[]> publicKeyCache;


    @Inject
    @CacheProducer
    @CacheType("WRONG_CACHE_NAME")
    Cache<String, Long> wrongCache;

    @BeforeEach
    void setUp() {

    }

    @Test
    void testWrongCacheNameInjection() {
        assertNull(wrongCache);
        assertNotNull(publicKeyCache);
    }

    @Test
    void testGuavaCacheInjection() {
        String key = "first";
        publicKeyCache.put(key, key.getBytes());

        byte[] value = publicKeyCache.getIfPresent(key);
        assertNotNull(value);
        assertArrayEquals(key.getBytes(), value);
        value = publicKeyCache.getIfPresent("second");
        assertNull(value);
    }

    private static class CacheLoaderProducer {
        @Produces
        public CacheLoader<Long, OrderFreezing> loader() {
            return mock(CacheLoader.class);
        }
    }

}
