/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.account;

import com.apollocurrency.aplwallet.apl.core.cache.AccountCacheConfig;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;
import com.apollocurrency.aplwallet.apl.util.cdi.config.Property;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcher;
import com.google.common.cache.Cache;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class AccountTableCacheConfiguration {
    private final TaskDispatchManager taskManager;
    private final InMemoryCacheManager cacheManager;
    private final AccountTable accountTable;
    @Getter
    private final boolean cacheEnabled;
    private Cache<DbKey, Account> accountTableCache;

    @Inject
    public AccountTableCacheConfiguration(InMemoryCacheManager cacheManager,
                                          TaskDispatchManager taskManager,
                                          AccountTableProducer producer,
                                          @Property(name = "apl.enableAccountCache") boolean cacheEnabled) {
        this.cacheManager = Objects.requireNonNull(cacheManager, "Cache manager is NULL");
        this.accountTable = producer.accountTable();
        this.taskManager = taskManager;
        this.cacheEnabled = cacheEnabled;
    }

    @PostConstruct
    void init() {
        if (isCacheEnabled()) {
            log.info("'{}' is TURNED ON...", AccountCacheConfig.CACHE_NAME);
            accountTableCache = cacheManager.acquireCache(AccountCacheConfig.CACHE_NAME);
            log.debug("--cache-- init ACCOUNT CACHE={}", accountTableCache);
            warmUp();

            TaskDispatcher taskDispatcher = taskManager.newScheduledDispatcher("AccountTableProducer-periodics");
            taskDispatcher.schedule(Task.builder()
                .name("Cache-stats")
                .initialDelay(30_000)
                .delay(180_000)
                .task(() -> log.info("--cache-- Account Cache size={} stats={}", accountTableCache.size(), accountTableCache.stats().toString()))
                .build());
        } else {
            log.info("'{}' is TURNED OFF...", AccountCacheConfig.CACHE_NAME);
        }
    }

    private void warmUp() {
        log.info("Warming up {}", AccountCacheConfig.CACHE_NAME);
        List<Account> recentAccounts = accountTable.getRecentAccounts(10_000);
        Map<DbKey, Account> groupedRecentAccounts = recentAccounts.stream().collect(Collectors.toMap(AccountTableInterface::newKey, Function.identity()));
        accountTableCache.putAll(groupedRecentAccounts);
        log.info("{} warm up is done with {} accounts", AccountCacheConfig.CACHE_NAME, groupedRecentAccounts.size());
    }

    void onPublicKeyAssigned(@Observes PublicKey publicKey) {
        if (isCacheEnabled()) {
            Account account = accountTableCache.getIfPresent(AccountTable.accountDbKeyFactory.newKey(publicKey.getAccountId()));
            if (account != null && !publicKey.equals(account.getPublicKey())) {
                log.info("--ACCOUNT CACHE-- Assign new public key {} for account {}", publicKey, account);
                account.setPublicKey(publicKey);
            }
        }
    }

    @Produces
    @Singleton
    public AccountTableInterface getTable() {
        if (isCacheEnabled()) {
            return new AccountCachedTable(accountTableCache, accountTable);
        } else {
            return accountTable;
        }
    }
}