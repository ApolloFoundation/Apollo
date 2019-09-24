/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.cache;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jboss.weld.environment.util.Collections;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.List;
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
    public <K, V> Cache<K, V> acquireCache(InjectionPoint injectionPoint){
        Annotated annotated = injectionPoint.getAnnotated();
        CacheType cacheTypeAnnotation = annotated.getAnnotation(CacheType.class);
        return acquireCache(cacheTypeAnnotation.value());
    }

    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> acquireCache(String cacheName){
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
        //  key#hashCode:int + value#reference
        int extra = 4        + newCalc().refExtra;
        int size = Math.max( (int) (availableMemory / sumPriority * config.getCachePriority() / (config.getExpectedElementSize() + extra)), 1) + 1;
        log.debug("Recalculate and set maxSize={} for cache {}", size, config.getCacheName());
        config.setMaxSize(size);
        return config.cacheBuilder().maximumSize(size);
    }

    public List<String> getAllocatedCacheNames(){
        return Collections.asList(inMemoryCaches.keys());
    }

    public CacheStats getStats(String cacheName){
        CacheStats stats = null;
        Cache cache = inMemoryCaches.get(cacheName);
        if (cache != null){
            stats = cache.stats();
        }
        return stats;
    }

    /**
     * Return new instance of memory calculator
     */
    public static MemoryUsageCalculator newCalc(){
        return new MemoryUsageCalculator().startObject();
    }

    /**
     * The utility class helps to calculate the memory usage of a Java object
     */
    @Getter
    public static class MemoryUsageCalculator {
        public static final int INT_SIZE = 4;
        public static final int LONG_SIZE = 8;
        public static final int CHAR_SIZE = 2;

        private static int arch;
        static {
            try {
                arch = Integer.parseInt(System.getProperty("sun.arch.data.model", "64"));
            } catch (NumberFormatException e) {
                arch = 64;
            }
        }
        private final int archPadding;
        private final int objectExtra;
        private final int refExtra;
        private final int arrayExtra;
        private final int stringExtra;
        private int mainPart;
        private int extraPart;

        public MemoryUsageCalculator() {
            this(arch);
        }

        public MemoryUsageCalculator(int arch) {
            Preconditions.checkArgument(arch == 32 || arch == 64, "Arch value %s must be equal 32 or 64", String.valueOf(arch));
            int archMult = arch/32;
            this.archPadding = 4 * archMult;
            this.refExtra = 4 * archMult;
            this.objectExtra = padding(archPadding, 4 + 4 * archMult);
            this.arrayExtra = objectExtra + padding(archPadding, INT_SIZE);
            this.stringExtra = objectExtra + padding(archPadding, refExtra + INT_SIZE + 1) + arrayExtra;
            this.mainPart = 0;
            this.extraPart = 0;
        }

        /**
         * All memory allocations are aligned to addresses that are divisible by 4 or 8
         * @param archPadding the int value: 4 or 8
         * @param size the memory block size
         * @return the padding byte count
         */
        private static int padding(int archPadding, int size){
            return size + (archPadding - (size % archPadding)) % archPadding;
        }

        /**
         * All memory allocations are aligned to addresses that are divisible by 4 or 8
         * @param size the memory block size
         * @return the padding byte count
         */
        private int padding(int size){
            return padding(this.archPadding, size);
        }

        /**
         * Add bytes of Object header
         * @return this calculator
         */
        public MemoryUsageCalculator startObject(){
            mainPart += objectExtra;
            return this;
        }

        /**
         * Add sum of byte to refer to object of particular size and add size padding.
         * @param size the size of object
         * @return this calculator
         */
        public MemoryUsageCalculator addAggregation(int size){
            mainPart += refExtra;
            extraPart += objectExtra + padding(size);
            return this;
        }

        /**
         * Add sum of bytes of reference to an object.
         * @return this calculator
         */
        public MemoryUsageCalculator addReference(){
            mainPart += refExtra;
            return this;
        }

        /**
         * Add sum of byte of reference to an object of particular size. Object is aligned.
         * @param size the size of object
         * @return this calculator
         */
        public MemoryUsageCalculator addReference(int size){
            mainPart += refExtra;
            extraPart += size;
            return this;
        }

        /**
         * Add sum of byte to refer to array of particular size and add size padding.
         * @param size the size of array in bytes
         * @return this calculator
         */
        public MemoryUsageCalculator addArrayExtra(int size){
            mainPart += refExtra;
            extraPart += padding(arrayExtra + size);
            return this;
        }

        /**
         * Add sum of byte to refer to String object of particular length.
         * @param stringLength the string length
         * @return this calculator
         */
        public MemoryUsageCalculator addString(int stringLength) {
            mainPart += refExtra;
            extraPart += stringExtra + padding(stringLength*CHAR_SIZE);
            return this;
        }

        /**
         * Add memory requirements of primitive type: boolean
         * @return this calculator
         */
        public MemoryUsageCalculator addBooleanPrimitive() {
            mainPart += 1;
            return this;
        }

        /**
         * Add memory requirements of primitive type: byte
         * @return this calculator
         */
        public MemoryUsageCalculator addBytePrimitive() {
            mainPart += 1;
            return this;
        }

        /**
         * Add memory requirements of primitive type: char
         * @return this calculator
         */
        public MemoryUsageCalculator addChar() {
            mainPart += CHAR_SIZE;
            return this;
        }

        /**
         * Add memory requirements of primitive type: int
         * @return this calculator
         */
        public MemoryUsageCalculator addInt() {
            mainPart += INT_SIZE;
            return this;
        }

        /**
         * Add memory requirements of primitive type: long
         * @return this calculator
         */
        public MemoryUsageCalculator addLongPrimitive() {
            mainPart += LONG_SIZE;
            return this;
        }

        /**
         * Add exactly defined memory requirements
         * @param size the added bytes
         * @return this calculator
         */
        public MemoryUsageCalculator add(int size){
            mainPart += size;
            return this;
        }

        /**
         * Calculate the memory requirements result
         * @return the total bytes count
         */
        public int calc(){
            return padding(mainPart) + extraPart;
        }
    }
}
