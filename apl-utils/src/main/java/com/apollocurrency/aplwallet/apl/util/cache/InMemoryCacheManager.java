/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.cache;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Singleton
public class InMemoryCacheManager {

    private static final long MIN_MEMORY_SIZE_FOR_CACHES = 16*1024*1024;//in bytes

    private ConcurrentHashMap<String, Cache> inMemoryCaches;

    @Inject
    public InMemoryCacheManager(InMemoryCacheConfigurator configurator) {
        Objects.requireNonNull(configurator, "Configurator is NULL.");
        validateConfigurations(configurator);
        allocateAllCaches(configurator);
    }

    @Produces @CacheProducer
    public <K, V> Cache<K, V> createCache(InjectionPoint injectionPoint){
        Annotated annotated = injectionPoint.getAnnotated();
        CacheType cacheTypeAnnotation = annotated.getAnnotation(CacheType.class);
        return createCache(cacheTypeAnnotation.value());
    }

    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> createCache(String cacheName){
        Cache<K,V> cache = inMemoryCaches.get(cacheName);
        if(cache == null){
            log.warn("Cache [{}] not found.", cacheName);
        }
        return cache;
    }

    private void validateConfigurations(InMemoryCacheConfigurator cfg){
        Preconditions.checkState(cfg.getAvailableMemory()>=MIN_MEMORY_SIZE_FOR_CACHES
                ,"The available memory is less than %s bytes.", MIN_MEMORY_SIZE_FOR_CACHES);

        cfg.getConfiguredCaches().stream().forEach(cacheConfiguration -> {
            Preconditions.checkArgument( StringUtils.isNotEmpty(cacheConfiguration.getCacheName()), "Cache name cant be empty.");
            Preconditions.checkArgument(cacheConfiguration.getExpectedElementSize()>0, "Element size must not be negative or zero.");
            Preconditions.checkArgument(cacheConfiguration.getCachePriority()>0,"Cache priority must be greater than zero.");
        });
    }

    @SuppressWarnings("unchecked")
    private void allocateAllCaches(InMemoryCacheConfigurator configurator){
        inMemoryCaches = new ConcurrentHashMap<>();
        final int sumPriority = configurator.getConfiguredCaches().stream().mapToInt(CacheConfiguration::getCachePriority).sum();
        configurator.getConfiguredCaches().forEach(config -> {
            CacheBuilder builder = configureCache(config, configurator.getAvailableMemory(), sumPriority);
            log.debug("Configured builder={}", builder);
            Cache cache;
            Optional<CacheLoader> loader = config.getCacheLoader();
            if (loader.isPresent()){
                cache = builder.build(loader.get());
            }else{
                cache = builder.build();
            }
            inMemoryCaches.put(config.getCacheName(), cache);
            log.debug("Allocated cache={}", config);
        });
    }

    private CacheBuilder configureCache(CacheConfiguration config, long availableMemory, int sumPriority){
        //       key#hashCode + value#reference
        int extra = 4         +      8;
        int size = Math.max( (int) (availableMemory / sumPriority * config.getCachePriority() / (config.getExpectedElementSize() + extra)), 1) + 1;
        log.debug("Recalculate and set maxSize={} for cache {}", size, config.getCacheName());
        config.setMaxSize(size);
        return config.cacheBuilder().maximumSize(size);
    }

}
