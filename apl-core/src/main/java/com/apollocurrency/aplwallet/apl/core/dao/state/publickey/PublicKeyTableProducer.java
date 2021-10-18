/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.publickey;

import com.apollocurrency.aplwallet.apl.core.cache.PublicKeyCacheConfig;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.CachedTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTableInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcher;
import com.google.common.cache.Cache;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Objects;

import static com.apollocurrency.aplwallet.apl.util.Constants.HEALTH_CHECK_INTERVAL;

@Slf4j
@Singleton
public class PublicKeyTableProducer {
    private final TaskDispatchManager taskManager;
    private final InMemoryCacheManager cacheManager;
    private final EntityDbTableInterface<PublicKey> publicKeyTable;
    private final EntityDbTableInterface<PublicKey> genesisPublicKeyTable;
    @Getter
    private final boolean cacheEnabled;
    private Cache<DbKey, PublicKey> publicKeyCache;

    @Inject
    public PublicKeyTableProducer(PropertiesHolder propertiesHolder,
                                  InMemoryCacheManager cacheManager,
                                  TaskDispatchManager taskManager,
                                  DatabaseManager databaseManager,
                                  Event<FullTextOperationData> fullTextOperationDataEvent) {
        this.cacheManager = Objects.requireNonNull(cacheManager, "Cache manager is NULL");
        Objects.requireNonNull(databaseManager);
        this.publicKeyTable = new PublicKeyTable(databaseManager, fullTextOperationDataEvent);
        this.genesisPublicKeyTable = new GenesisPublicKeyTable(databaseManager, fullTextOperationDataEvent);
        this.taskManager = taskManager;
        this.cacheEnabled = propertiesHolder.getBooleanProperty("apl.enablePublicKeyCache");
    }

    @PostConstruct
    private void init() {
        //todo warm up the cache APL-1726
        if (isCacheEnabled()) {
            log.info("'{}' is TURNED ON...", PublicKeyCacheConfig.PUBLIC_KEY_CACHE_NAME);
            publicKeyCache = cacheManager.acquireCache(PublicKeyCacheConfig.PUBLIC_KEY_CACHE_NAME);
            log.debug("--cache-- init PUBLIC KEY CACHE={}", publicKeyCache);
            TaskDispatcher taskDispatcher = taskManager.newScheduledDispatcher("PublicKeyProducer-periodics");
            taskDispatcher.schedule(Task.builder()
                .name("Cache-stats")
                .initialDelay(HEALTH_CHECK_INTERVAL * 2)
                .delay(HEALTH_CHECK_INTERVAL)
                .task(() -> log.info("--cache-- PUBLIC Keys Cache size={} stats={}", publicKeyCache.size(), publicKeyCache.stats().toString()))
                .build());
        } else {
            log.info("'{}' is TURNED OFF...", PublicKeyCacheConfig.PUBLIC_KEY_CACHE_NAME);
        }
    }

    @Produces
    @Named("publicKeyTable")
    public EntityDbTableInterface<PublicKey> getPublicKeyTable() {
        if (isCacheEnabled()) {
            return new CachedTable<>(publicKeyCache, publicKeyTable);
        } else {
            return publicKeyTable;
        }
    }

    @Produces
    @Named("genesisPublicKeyTable")
    public EntityDbTableInterface<PublicKey> getGenesisPublicKeyTable() {
        if (isCacheEnabled()) {
            return new CachedTable<>(publicKeyCache, genesisPublicKeyTable);
        } else {
            return genesisPublicKeyTable;
        }
    }

}
