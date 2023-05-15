/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.cache;

import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import com.google.common.cache.CacheStats;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SynchronizedCacheTest {

    SynchronizedCache<Long, CachedValue> cache;
    private CachedValue nullNamedValue = new CachedValue(null, new byte[32], 1);
    private CachedValue supermanValue  = new CachedValue("superman", null, 2);

    @BeforeEach
    void setUp() {
        cache = new SynchronizedCache<>(2);
        cache.put(1L, nullNamedValue);
        ThreadUtils.sleep(1);
        cache.put(2L, supermanValue);
    }

    @Test
    void size() {
        long size = cache.size();

        assertEquals(2, size);
    }

    @Test
    void getIfPresent() {
        CachedValue value = cache.getIfPresent(2L);

        assertEquals(value, supermanValue);
    }

    @Test
    void put_withEviction() {
        CachedValue batman = new CachedValue("batman", null, 3L);

        cache.put(3L, batman);

        assertEquals(2, cache.size());
        CachedValue evictedValue = cache.getIfPresent(1L);
        assertNull(evictedValue, "Value should be evicted during put operation, because max size of the cache reached");
        assertEquals(batman, cache.getIfPresent(3L));
        CacheStats stats = cache.stats();
        assertEquals(1, stats.evictionCount());
        assertEquals(1, stats.missCount());
        assertEquals(1, stats.hitCount());
    }

    @Test
    void put_replaceSameValue() {
        CachedValue newSupermanValue = new CachedValue("superman", new byte[100], 2L);

        cache.put(2L, newSupermanValue);

        assertEquals(2, cache.size());
        ConcurrentMap<Long, CachedValue> allValues = cache.asMap();
        assertEquals(2, allValues.size());
        assertEquals(Map.of(2L, newSupermanValue, 1L, nullNamedValue), allValues);
        assertEquals(newSupermanValue, cache.getIfPresent(2L));
        verifyCacheStats(1, 0, 0);
    }

    @Test
    void invalidate() {
        cache.invalidate(2L);

        assertEquals(1, cache.size());
        CachedValue invalidated = cache.getIfPresent(2L);
        assertNull(invalidated, "Value should not be present in the cache after invalidation");
    }

    @Test
    void invalidateAll() {
        cache.invalidateAll();

        assertEquals(0, cache.size());
    }

    @Test
    void wrongArgumentsUsage() {
        assertThrows(IllegalArgumentException.class, () -> new SynchronizedCache<>(0)); // size should be greater than 0
        assertThrows(NullPointerException.class, () -> cache.getIfPresent(null));
        assertThrows(NullPointerException.class, () -> cache.put(null, nullNamedValue));
        assertThrows(NullPointerException.class, () -> cache.put(2L, null));
        assertThrows(NullPointerException.class, () -> cache.invalidate(null));

    }

    @AllArgsConstructor
    @Data
    private class CachedValue {
        private String name;
        private byte[] array;
        private long id;
    }

    private void verifyCacheStats(int hitCount, int missCount, int evictionCount) {
        CacheStats stats = cache.stats();
        assertEquals(hitCount, stats.hitCount());
        assertEquals(missCount, stats.missCount());
        assertEquals(evictionCount, stats.evictionCount());
    }
}