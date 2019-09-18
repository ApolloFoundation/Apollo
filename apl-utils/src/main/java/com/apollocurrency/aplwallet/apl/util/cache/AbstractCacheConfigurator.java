/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.cache;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;

import java.util.Optional;

public abstract class AbstractCacheConfigurator<K, V> implements CacheConfiguration<K, V> {
    private String name;
    private long elementSize;
    private int percentCapacity;
    private int maxSize = -1;

    public AbstractCacheConfigurator(String name, long elementSize, int percentCapacity) {
        this.name = name;
        this.elementSize = elementSize;
        this.percentCapacity = percentCapacity;
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
    public abstract CacheBuilder builder();

    @Override
    public Optional<CacheLoader<K, V>> getCacheLoader(){
        return Optional.empty();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("elementSize", elementSize)
                .add("percentCapacity", percentCapacity)
                .add("maxSize", maxSize)
                .add("builder", builder().toString())
                .toString();
    }
}
