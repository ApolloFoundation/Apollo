/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.account.dao.GenesisPublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.account.dao.PublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.account.model.PublicKey;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.cache.PublicKeyCacheConfig;
import com.apollocurrency.aplwallet.apl.core.db.derived.CachedTable;
import com.apollocurrency.aplwallet.apl.core.db.derived.EntityDbTableInterface;
import com.apollocurrency.aplwallet.apl.core.task.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcher;
import com.google.common.cache.Cache;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Objects;

import static com.apollocurrency.aplwallet.apl.util.Constants.HEALTH_CHECK_INTERVAL;

@Slf4j
@Singleton
public class PublicKeyTableProducer {
    private final InMemoryCacheManager cacheManager;

    private final EntityDbTableInterface<PublicKey> publicKeyTable;
    private final EntityDbTableInterface<PublicKey> genesisPublicKeyTable;

    private final TaskDispatchManager taskManager;

    @Getter
    private final boolean cacheEnabled;
    private Cache<DbKey, PublicKey> publicKeyCache;

    @Inject
    public PublicKeyTableProducer(PropertiesHolder propertiesHolder,
                                  Blockchain blockchain,
                                  InMemoryCacheManager cacheManager,
                                  TaskDispatchManager taskManager) {
        Objects.requireNonNull(blockchain, "Block chain is NULL.");
        this.cacheManager = Objects.requireNonNull(cacheManager, "Cache manager is NULL");
        this.publicKeyTable = new PublicKeyTable(blockchain);
        this.genesisPublicKeyTable = new GenesisPublicKeyTable(blockchain);
        this.taskManager = taskManager;
        this.cacheEnabled = propertiesHolder.getBooleanProperty("apl.enablePublicKeyCache");
    }

    @PostConstruct
    private void init() {
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
