/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Slf4j
class InMemoryCacheManagerTest {

    private InMemoryCacheConfigurator configurator = mock(InMemoryCacheConfigurator.class);
    private InMemoryCacheManager manager;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testWrongConfiguration_whenAvailableMemoryLessThan16Mb() {
        doReturn(15*1024*1024L).when(configurator).getAvailableMemory();
        assertThrows(IllegalStateException.class, () -> new InMemoryCacheManager(configurator));
    }

    @Test
    void testWrongConfiguration_whenElementSizeNegative() {
        doReturn(64*1024*1024L).when(configurator).getAvailableMemory();
        doReturn(List.of(new SimpleCacheConfigurator("SIMPLE_CACHE_NAME_16", 0, 50))).
                when(configurator).getConfiguredCaches();
        assertThrows(IllegalArgumentException.class, () -> new InMemoryCacheManager(configurator));
    }

    @Test
    void testWrongConfiguration_whenSummaryCapacityExceeds100Percent() {
        doReturn(64*1024*1024L).when(configurator).getAvailableMemory();
        doReturn(List.of(new SimpleCacheConfigurator("SIMPLE_CACHE_NAME_16", 16, 49),
                new SimpleCacheConfigurator("SIMPLE_CACHE_NAME_128", 128, 31),
                new SimpleCacheConfigurator("SIMPLE_CACHE_NAME_1024", 1024, 21)
        )).when(configurator).getConfiguredCaches();
        assertThrows(IllegalArgumentException.class, () -> new InMemoryCacheManager(configurator));
    }

    @Test
    void createCache() {
        setupManager();
        Cache<String, byte[]> cache1024 = manager.createCache("SIMPLE_CACHE_NAME_1024");
        assertNotNull(cache1024);

        cache1024 = manager.createCache("SIMPLE_CACHE_NAME_2048");
        assertNull(cache1024);
    }

    @Test
    void testCacheManipulations() {
        setupManager();
        byte[] bytes1 = "1234567890".getBytes();
        byte[] bytes2 = "ABCDEF1234567890".getBytes();
        Cache<String, byte[]> cache1024 = manager.createCache("SIMPLE_CACHE_NAME_1024");
        assertNotNull(cache1024);
        byte[] rez = cache1024.getIfPresent("key1");
        assertNull(rez);
        cache1024.put("key1", bytes1);
        cache1024.put("key2", bytes2);
        assertEquals(2, cache1024.size());
        assertEquals(bytes1, cache1024.getIfPresent("key1"));
        assertEquals(bytes2, cache1024.getIfPresent("key2"));
    }

    @Test
    void testCacheEvictions() {
        String cacheName = "SIMPLE_CACHE_NAME";
        doReturn(64*1024*1024L).when(configurator).getAvailableMemory();
        CacheConfiguration cacheCfg = new SimpleCacheConfigurator(cacheName, 1024*1024, 100);
        doReturn(List.of(cacheCfg)).when(configurator).getConfiguredCaches();
        manager = new InMemoryCacheManager(configurator);

        Cache<Integer, byte[]> cache = manager.createCache(cacheName);
        assertEquals(64, cacheCfg.getMaxSize());
        int i=0;
        for(; i<cacheCfg.getMaxSize()*2;i++){
          cache.put(i, Long.toUnsignedString(i*1000L).getBytes());
        }
        assertEquals(64*2, i);
        assertEquals(64, cache.size());
        i--;
        assertNotNull(cache.getIfPresent(i));
        assertArrayEquals(Long.toUnsignedString(i*1000L).getBytes(), cache.getIfPresent(i));
    }

    private void setupManager() {
        doReturn(64*1024*1024L).when(configurator).getAvailableMemory();
        doReturn(List.of(new SimpleCacheConfigurator("SIMPLE_CACHE_NAME_16", 16, 50),
                new SimpleCacheConfigurator("SIMPLE_CACHE_NAME_128", 128, 30),
                new SimpleCacheConfigurator("SIMPLE_CACHE_NAME_1024", 1024, 20)
        )).when(configurator).getConfiguredCaches();
        manager = new InMemoryCacheManager(configurator);
    }

    class SimpleCacheConfigurator extends CacheConfigurator {

        public SimpleCacheConfigurator(String name, long elementSize, int percentCapacity) {
            super(name, elementSize, percentCapacity);
        }

        @Override
        public CacheBuilder cacheBuilder() {
            return CacheBuilder.newBuilder();
        }
    }

}
