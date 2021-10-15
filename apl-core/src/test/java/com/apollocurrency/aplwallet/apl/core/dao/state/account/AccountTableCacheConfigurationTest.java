/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.account;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcher;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
        mockCacheAndDispatcher();
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

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> cacheConfigurer.init());

        assertTrue("Map collector should throw merge error exception", ex.getMessage().startsWith("Duplicate key LongKey{id=800}"));
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

    @Test
    void testPublicKeyAssignedEvents_cacheEnabled_accountCached_noPublicKey() {
        setUp(true);
        mockCacheAndDispatcher();
        cacheConfigurer.init();
        PublicKey newAcc0Key = new PublicKey(td.ACC_0.getId(), new byte[32], td.ACC_0.getHeight() + 1);
        when(cache.getIfPresent(new LongKey(td.ACC_0.getId()))).thenReturn(td.ACC_0);

        cacheConfigurer.onPublicKeyAssigned(newAcc0Key);

        assertEquals(newAcc0Key, td.ACC_0.getPublicKey());
    }

    @Test
    void testPublicKeyAssignedEvents_cacheDisabled() {
        setUp(false);
        cacheConfigurer.init();
        PublicKey newAcc0Key = new PublicKey(td.ACC_0.getId(), new byte[32], td.ACC_0.getHeight() + 1);

        cacheConfigurer.onPublicKeyAssigned(newAcc0Key);

        verifyNoInteractions(cache, accountTable);
        assertNull("Account with id 50 should no have an assigned public key, because cache is not enabled", td.ACC_0.getPublicKey());
    }

    @Test
    void testPublicKeyAssignedEvents_cacheEnabled_noAccount() {
        setUp(true);
        mockCacheAndDispatcher();
        cacheConfigurer.init();
        PublicKey newAcc0Key = new PublicKey(td.ACC_0.getId(), new byte[32], td.ACC_0.getHeight() + 1);

        cacheConfigurer.onPublicKeyAssigned(newAcc0Key);

        assertNull("Account with id 50 should no have an assigned public key, because that account is not cached", td.ACC_0.getPublicKey());
    }

    @Test
    void testPublicKeyAssignedEvents_cacheEnabled_accountCached_publicKeyExistWithNoKey() {
        setUp(true);
        mockCacheAndDispatcher();

        cacheConfigurer.init();
        PublicKey newAcc0Key = new PublicKey(td.ACC_0.getId(), new byte[32], td.ACC_0.getHeight() + 1);
        when(cache.getIfPresent(new LongKey(td.ACC_0.getId()))).thenReturn(td.ACC_0);
        td.ACC_0.setPublicKey(new PublicKey(td.ACC_0.getId(), null, td.ACC_0.getHeight()));

        cacheConfigurer.onPublicKeyAssigned(newAcc0Key);

        assertEquals(newAcc0Key, td.ACC_0.getPublicKey());
    }

    @Test
    void testPublicKeyAssignedEvents_cacheEnabled_accountCached_publicKeyExistWithLowerHeight() {
        setUp(true);
        mockCacheAndDispatcher();
        doReturn(List.of(td.ACC_0, td.ACC_10, td.ACC_9)).when(accountTable).getRecentAccounts(10_000);
        cacheConfigurer.init();
        PublicKey newAcc0Key = new PublicKey(td.ACC_0.getId(), new byte[32], td.ACC_0.getHeight() + 1);
        when(cache.getIfPresent(new LongKey(td.ACC_0.getId()))).thenReturn(td.ACC_0);
        td.ACC_0.setPublicKey(new PublicKey(td.ACC_0.getId(), new byte[32], td.ACC_0.getHeight())); // existing key

        cacheConfigurer.onPublicKeyAssigned(newAcc0Key);

        assertEquals(newAcc0Key, td.ACC_0.getPublicKey());
    }

    @Test
    void testPublicKeyAssignedEvents_cacheEnabled_accountCached_samePublicKeyExist() {
        setUp(true);
        mockCacheAndDispatcher();
        doReturn(List.of(td.ACC_0, td.ACC_10, td.ACC_9)).when(accountTable).getRecentAccounts(10_000);
        cacheConfigurer.init();
        PublicKey newAcc0Key = new PublicKey(td.ACC_0.getId(), new byte[32], td.ACC_0.getHeight() + 1);
        PublicKey existing = newAcc0Key.deepCopy();
        td.ACC_0.setPublicKey(existing);
        when(cache.getIfPresent(new LongKey(td.ACC_0.getId()))).thenReturn(td.ACC_0);

        cacheConfigurer.onPublicKeyAssigned(newAcc0Key);

        assertEquals(newAcc0Key, td.ACC_0.getPublicKey());
    }

    private void mockCacheAndDispatcher() {
        doReturn(cache).when(inMemoryCacheManager).acquireCache("ACCOUNT_CACHE");
        doReturn(taskDispatcher).when(taskDispatchManager).newScheduledDispatcher("AccountTableProducer-periodics");
        doAnswer(a-> {
            ((Task) a.getArgument(0)).run(); // simulate scheduled healtcheck execution and ensure no npe or other errors
            return true;
        }).when(taskDispatcher).schedule(any());
        when(cache.stats()).thenReturn(mock(CacheStats.class));
    }
}