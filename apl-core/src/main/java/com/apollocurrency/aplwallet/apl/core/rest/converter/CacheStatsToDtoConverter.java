/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.CacheStatsDTO;
import com.google.common.cache.CacheStats;

public class CacheStatsToDtoConverter implements Converter<CacheStats, CacheStatsDTO> {
    @Override
    public CacheStatsDTO apply(CacheStats cacheStats) {
        CacheStatsDTO dto = new CacheStatsDTO(
                cacheStats.hitCount(), cacheStats.hitRate(),
                cacheStats.missCount(), cacheStats.missRate(),
                cacheStats.loadSuccessCount(),
                cacheStats.loadSuccessCount(),
                cacheStats.totalLoadTime(),
                cacheStats.evictionCount()
                );

        return dto;
    }
}
