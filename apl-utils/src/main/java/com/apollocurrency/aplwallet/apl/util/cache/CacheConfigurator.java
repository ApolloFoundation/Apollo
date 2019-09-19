/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.cache;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;

import java.util.Optional;

public class CacheConfigurator<K, V> implements CacheConfiguration<K, V> {
    private String name;
    private long elementSize;
    private int percentCapacity;
    private int maxSize = -1;
    private CacheBuilder cacheBuilder;
    private Optional<CacheLoader<K, V>> optionalCacheLoader;

    public CacheConfigurator(String name, long elementSize, int percentCapacity) {
        this(name, elementSize, percentCapacity, null);
    }

    public CacheConfigurator(String name, long elementSize, int percentCapacity, CacheLoader<K, V> cacheLoader) {
        this.name = name;
        this.elementSize = elementSize;
        this.percentCapacity = percentCapacity;
        this.cacheBuilder = CacheBuilder.newBuilder();
        this.optionalCacheLoader = Optional.ofNullable(cacheLoader);
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
    public int getCacheCapacity() {
        return percentCapacity;
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
        return optionalCacheLoader;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("elementSize", elementSize)
                .add("percentCapacity", percentCapacity)
                .add("maxSize", maxSize)
                .add("cacheBuilder", cacheBuilder().toString())
                .toString();
    }
}
