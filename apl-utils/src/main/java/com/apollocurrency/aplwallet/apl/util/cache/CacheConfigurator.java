/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.cache;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;

import java.util.Objects;
import java.util.Optional;

public class CacheConfigurator<K, V> implements CacheConfiguration<K, V> {
    private final String name;
    private final long elementSize;
    private final int cachePriority;
    private int maxSize = -1;
    private final CacheBuilder<?,?> cacheBuilder;
    private final CacheLoader<K, V> cacheLoader;
    private final boolean useSynchronizedCache;


    public CacheConfigurator(String name, long elementSize, int cachePriority) {
        this(name, elementSize, cachePriority, null);
    }

    public CacheConfigurator(String name, long elementSize, int cachePriority, CacheLoader<K, V> cacheLoader) {
        this(name, elementSize, cachePriority, cacheLoader, false);
    }

    public CacheConfigurator(String name, long elementSize, int cachePriority, CacheLoader<K, V> cacheLoader, boolean useSynchronizedCache) {
        Objects.requireNonNull(name, "Cache name is NULL.");
        this.name = name;
        this.elementSize = elementSize;
        this.cachePriority = cachePriority;
        this.cacheBuilder = CacheBuilder.newBuilder();
        if (cacheLoader != null && useSynchronizedCache) {
            throw new IllegalArgumentException("Cache loader cannot be used for the synchronized cache");
        }
        this.useSynchronizedCache = useSynchronizedCache;
        this.cacheLoader = cacheLoader;
    }

    @Override
    public String getCacheName() {
        return name;
    }

    @Override
    public long getExpectedElementSize() {
        return elementSize;
    }

    @Override
    public int getCachePriority() {
        return cachePriority;
    }

    @Override
    public int getMaxSize() {
        return maxSize;
    }

    @Override
    public void setMaxSize(int maxSize) {
        Preconditions.checkState(this.maxSize == -1,
            "maximum size was already set to %s", this.maxSize);
        this.maxSize = maxSize;
    }

    @Override
    public CacheBuilder<?,?> cacheBuilder() {
        if (useSynchronizedCache) {
            throw new UnsupportedOperationException("Cache builder is not supported for the synchronized cache");
        }
        return cacheBuilder;
    }

    @Override
    public boolean shouldBeSynchronized() {
        return useSynchronizedCache;
    }

    @Override
    public Optional<CacheLoader<K, V>> getCacheLoader() {
        return Optional.ofNullable(cacheLoader);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("name", name)
            .add("elementSize", elementSize)
            .add("cachePriority", cachePriority)
            .add("maxSize", maxSize)
            .toString();
    }
}
