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
    private String name;
    private long elementSize;
    private int cachePriority;
    private int maxSize = -1;
    private CacheBuilder cacheBuilder;
    private CacheLoader<K, V> cacheLoader;

    public CacheConfigurator(String name, long elementSize, int cachePriority) {
        this(name, elementSize, cachePriority, null);
    }

    public CacheConfigurator(String name, long elementSize, int cachePriority, CacheLoader<K, V> cacheLoader) {
        Objects.requireNonNull(name, "Cache name is NULL.");
        this.name = name;
        this.elementSize = elementSize;
        this.cachePriority = cachePriority;
        this.cacheBuilder = CacheBuilder.newBuilder();
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
    public void setMaxSize(int maxSize){
        Preconditions.checkState( this.maxSize == -1,
                "maximum size was already set to %s", this.maxSize);
        this.maxSize = maxSize;
    }

    @Override
    public CacheBuilder cacheBuilder(){
        return cacheBuilder;
    }

    @Override
    public Optional<CacheLoader<K, V>> getCacheLoader(){
        return Optional.ofNullable(cacheLoader);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("elementSize", elementSize)
                .add("cachePriority", cachePriority)
                .add("maxSize", maxSize)
                .add("cacheBuilder", cacheBuilder().toString())
                .toString();
    }
}
