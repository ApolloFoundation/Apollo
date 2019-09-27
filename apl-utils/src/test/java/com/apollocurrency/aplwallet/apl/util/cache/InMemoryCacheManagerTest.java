/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager.MemoryUsageCalculator.LONG_SIZE;
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
        doReturn(List.of(new CacheConfigurator("SIMPLE_CACHE_NAME_16", 0, 50))).
                when(configurator).getConfiguredCaches();
        assertThrows(IllegalArgumentException.class, () -> new InMemoryCacheManager(configurator));
    }

    @Test
    void testWrongConfiguration_whenCachePriorityIsZero() {
        doReturn(64*1024*1024L).when(configurator).getAvailableMemory();
        doReturn(List.of(new CacheConfigurator("SIMPLE_CACHE_NAME_16", 16, 0))).
                when(configurator).getConfiguredCaches();
        assertThrows(IllegalArgumentException.class, () -> new InMemoryCacheManager(configurator));
    }

    @Test
    void createCache() {
        setupManager();
        Cache<String, byte[]> cache1024 = manager.acquireCache("SIMPLE_CACHE_NAME_1024");
        assertNotNull(cache1024);

        cache1024 = manager.acquireCache("SIMPLE_CACHE_NAME_2048");
        assertNull(cache1024);
    }

    @Test
    void testCacheManipulations() {
        setupManager();
        byte[] bytes1 = "1234567890".getBytes();
        byte[] bytes2 = "ABCDEF1234567890".getBytes();
        Cache<String, byte[]> cache1024 = manager.acquireCache("SIMPLE_CACHE_NAME_1024");
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
    void testCacheManipulationsWithCacheLoader() throws ExecutionException {
        String cacheName = "SIMPLE_CACHE_NAME";
        doReturn(64*1024*1024L).when(configurator).getAvailableMemory();
        CacheConfiguration<String, byte[]> cacheCfg = new CacheConfigurator<>(
                cacheName,
                1024,
                10,
                new CacheLoader<>() {
                    @Override
                    public byte[] load(String key) throws Exception {
                        return key.getBytes();
                    }
                });
        doReturn(List.of(cacheCfg)).when(configurator).getConfiguredCaches();
        manager = new InMemoryCacheManager(configurator);

        Cache<String, byte[]> cache = manager.acquireCache(cacheName);
        assertEquals(0L, cache.size());
        LoadingCache<String, byte[]> loadingCache = (LoadingCache<String, byte[]>) cache;
        String key1 = "firstKey";
        String key2 = "secondKey";
        assertArrayEquals(key1.getBytes(), loadingCache.get(key1));
        assertEquals(1L, cache.size());
        assertArrayEquals(key2.getBytes(), loadingCache.get(key2));
        assertEquals(2L, loadingCache.size());
    }

    @Test
    void testCacheEvictions() {
        String cacheName = "SIMPLE_CACHE_NAME";
        doReturn(64*1024*1024L).when(configurator).getAvailableMemory();
        CacheConfiguration cacheCfg = new CacheConfigurator(cacheName, 1024*1024, 1);
        doReturn(List.of(cacheCfg)).when(configurator).getConfiguredCaches();
        manager = new InMemoryCacheManager(configurator);

        Cache<Integer, byte[]> cache = manager.acquireCache(cacheName);
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

    @Test
    void testMemCalculator(){
        int size = new InMemoryCacheManager.MemoryUsageCalculator(64)
                .startObject()//+16
                .addBooleanPrimitive() //+1
                .addBytePrimitive()//+1
                .addChar()//+2
                .addInt() //+4
                .addLongPrimitive() //+8
                .addReference()// +8
                .addAggregation(LONG_SIZE) //8 + 16 + 8
                .addArrayExtra(32) //8 + 24 + 32
                .addString(5)//8 + 56 + 2*5 + 6
                .addReference(
                        new InMemoryCacheManager.MemoryUsageCalculator(64)
                        .startObject()
                        .addLongPrimitive()
                        .calc()
                )// 8 + 16 + 8
                .calc();//248
        assertEquals(248, size);
    }

    private void setupManager() {
        doReturn(64*1024*1024L).when(configurator).getAvailableMemory();
        doReturn(List.of(new CacheConfigurator("SIMPLE_CACHE_NAME_16", 16, 50),
                new CacheConfigurator("SIMPLE_CACHE_NAME_128", 128, 30),
                new CacheConfigurator("SIMPLE_CACHE_NAME_1024", 1024, 50)
        )).when(configurator).getConfiguredCaches();
        manager = new InMemoryCacheManager(configurator);
    }

}
