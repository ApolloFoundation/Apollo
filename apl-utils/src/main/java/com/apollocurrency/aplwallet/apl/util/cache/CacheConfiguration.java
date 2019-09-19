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

    int getCacheCapacity();

    int getMaxSize();

    void setMaxSize(int maxSize);

    Optional<CacheLoader<K, V>> getCacheLoader();

    CacheBuilder builder();
}
