/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;

import java.util.Optional;

public interface CacheConfiguration<K, V> {

    String getCacheName();

    long getExpectedElementSize();

    int getCachePriority();

    int getMaxSize();

    /**
     * Set the maximum size of cache, don't use manually, used in {@link InMemoryCacheManager} only.
     * @param maxSize the maximum size of cache
     */
    void setMaxSize(int maxSize);

    CacheBuilder cacheBuilder();

    Optional<CacheLoader<K, V>> getCacheLoader();

}
