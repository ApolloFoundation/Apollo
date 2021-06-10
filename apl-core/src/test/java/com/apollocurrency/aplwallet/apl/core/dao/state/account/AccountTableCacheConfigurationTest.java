/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.account;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcher;
import com.google.common.cache.Cache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AccountTableCacheConfigurationTest {
    @Mock
    InMemoryCacheManager inMemoryCacheManager;
    @Mock
    TaskDispatchManager taskDispatchManager;
    @Mock
    TaskDispatcher taskDispatcher;
    @Mock
    Cache cache;
    @Mock
    AccountTable accountTable;
    @Mock
    AccountTableProducer tableProducer;

    AccountTableCacheConfiguration cacheConfigurer;
    AccountTestData td;


    void setUp(boolean enableCache) {
        doReturn(accountTable).when(tableProducer).accountTable();
        cacheConfigurer = new AccountTableCacheConfiguration(inMemoryCacheManager, taskDispatchManager, tableProducer, enableCache);
        td = new AccountTestData();
    }


    @Test
    void initWithCache() {
        setUp(true);

        doReturn(cache).when(inMemoryCacheManager).acquireCache("ACCOUNT_CACHE");
        doReturn(taskDispatcher).when(taskDispatchManager).newScheduledDispatcher("AccountTableProducer-periodics");
        doReturn(List.of(td.ACC_0, td.ACC_10, td.ACC_9)).when(accountTable).getRecentAccounts(10_000);
        doReturn("account_mock_table").when(accountTable).getName();

        cacheConfigurer.init();

        AccountTableInterface table = cacheConfigurer.getTable();
        assertTrue("Account table should be cacheable, since the cache is enabled", table instanceof AccountCachedTable);
        Map<LongKey, Account> expectedCacheMap = Map.of(new LongKey(td.ACC_0.getId()), td.ACC_0,
            new LongKey(td.ACC_9.getId()), td.ACC_9, new LongKey(td.ACC_10.getId()), td.ACC_10);
        verify(cache).putAll(expectedCacheMap);
        verify(taskDispatcher).schedule(any(Task.class));
    }

    @Test
    void initWithCache_duplicateAccounts() {
        setUp(true);

        doReturn(cache).when(inMemoryCacheManager).acquireCache("ACCOUNT_CACHE");
        doReturn(List.of(td.ACC_0, td.ACC_12, td.ACC_11)).when(accountTable).getRecentAccounts(10_000);

        assertThrows(IllegalStateException.class, () -> cacheConfigurer.init());

        verify(cache, never()).putAll(anyMap());
        verify(taskDispatcher, never()).schedule(any(Task.class));
    }

    @Test
    void initWithoutCache() {
        setUp(false);

        cacheConfigurer.init();

        AccountTableInterface table = cacheConfigurer.getTable();
        assertTrue("Expected AccountTable type for non-cached account table", table instanceof AccountTable);

        verifyNoInteractions(cache, taskDispatchManager, inMemoryCacheManager);
    }
}