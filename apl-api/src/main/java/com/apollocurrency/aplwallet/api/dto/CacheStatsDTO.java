/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.api.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@NoArgsConstructor
@RequiredArgsConstructor
@Data
@Schema(name = "CacheStats", description = "Statistics about the performance of a Cache.")
public class CacheStatsDTO {
    @Schema(name = "cacheName", description = "The cahce name.")
    private String cacheName;
    @NonNull
    @Schema(name = "hitCount", description = "The number of times Cache lookup methods have returned a cached value.")
    private long hitCount;
    @NonNull
    @Schema(name = "hitRate", description = "The ratio of cache requests which were hits. This is defined as hitCount / requestCount, or 1.0 when requestCount == 0. Note that hitRate + missRate =~ 1.0.")
    private double hitRate;
    @NonNull
    @Schema(name = "missCount", description = "The number of times Cache lookup methods have returned an uncached (newly loaded) value, or null. ")
    private long missCount;
    @NonNull
    @Schema(name = "missRate", description = "The ratio of cache requests which were misses. This is defined as missCount / requestCount, or 0.0 when requestCount == 0. Note that hitRate + missRate =~ 1.0.")
    private double missRate;
    @NonNull
    @Schema(name = "loadSuccessCount", description = "The number of times Cache lookup methods have successfully loaded a new value.")
    private long loadSuccessCount;
    @NonNull
    @Schema(name = "loadExceptionCount", description = "The number of times Cache lookup methods threw an exception while loading a new value.")
    private long loadExceptionCount;
    @NonNull
    @Schema(name = "totalLoadTime", description = "The total number of nanoseconds the cache has spent loading new values. This can be used to calculate the miss penalty. This value is increased every time loadSuccessCount or loadExceptionCount is incremented.")
    private long totalLoadTime;
    @NonNull
    @Schema(name = "evictionCount", description = "The number of times an entry has been evicted.")
    private long evictionCount;
}
