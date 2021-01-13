package com.apollocurrency.aplwallet.apl.core.cache;

import com.apollocurrency.aplwallet.apl.util.cache.CacheProducer;
import com.apollocurrency.aplwallet.apl.util.cache.CacheType;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheConfigurator;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;
import com.google.common.cache.Cache;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class InMemoryCDICacheFactory {
    private final InMemoryCacheManager cacheManager;

    @Inject
    public InMemoryCDICacheFactory(InMemoryCacheConfigurator configurator) {
        this.cacheManager = new InMemoryCacheManager(configurator);
    }


    @Produces
    @Singleton
    public InMemoryCacheManager cacheManager() {
        return cacheManager;
    }
    @Produces
    @CacheProducer
    public <K, V> Cache<K, V> acquireCache(InjectionPoint injectionPoint) {
        Annotated annotated = injectionPoint.getAnnotated();
        CacheType cacheTypeAnnotation = annotated.getAnnotation(CacheType.class);
        return this.cacheManager.acquireCache(cacheTypeAnnotation.value());
    }
}
