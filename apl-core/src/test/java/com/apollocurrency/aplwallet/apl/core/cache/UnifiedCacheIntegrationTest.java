/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.cache;

import com.apollocurrency.aplwallet.apl.util.cache.CacheProducer;
import com.apollocurrency.aplwallet.apl.util.cache.CacheType;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;
import com.google.common.cache.Cache;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static com.apollocurrency.aplwallet.apl.core.cache.PublicKeyCacheConfig.PUBLIC_KEY_CACHE_NAME;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@EnableWeld
public class UnifiedCacheIntegrationTest {

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(AplCacheConfig.class, InMemoryCacheManager.class).build();

    @Inject
    @CacheProducer
    @CacheType(PUBLIC_KEY_CACHE_NAME)
    Cache<String, byte[]> publicKeyCache;


    @Inject @CacheProducer @CacheType("WRONG_CACHE_NAME")
    Cache<String, Long> wrongCache;

    @BeforeEach
    void setUp() {

    }

    @Test
    void testWrongCacheNameInjection(){
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

}
