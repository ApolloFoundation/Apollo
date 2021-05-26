/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.account;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import com.google.common.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
@ExtendWith(MockitoExtension.class)
class AccountCachedTableTest {
    @Mock
    AccountTable accountTable;
    @Mock
    Cache cache;
    AccountCachedTable cachedTable;
    AccountTestData td;

    @BeforeEach
    void setUp() {
        cachedTable = new AccountCachedTable(cache, accountTable);
        td = new AccountTestData();
    }
    @Test
    void selectAllForKey() throws SQLException {
        doReturn(List.of(td.ACC_0)).when(accountTable).selectAllForKey(1L);

        List<Account> accounts = cachedTable.selectAllForKey(1L);

        assertEquals(List.of(td.ACC_0), accounts);

    }

    @Test
    void getTotalSupply() {
        doReturn(1_000_000L).when(accountTable).getTotalSupply(1000L);

        long totalSupply = cachedTable.getTotalSupply(1000L);

        assertEquals(1_000_000L, totalSupply);
    }

    @Test
    void getTopHolders() {
        doReturn(List.of(td.ACC_10)).when(accountTable).getTopHolders(1);

        List<Account> accounts = cachedTable.getTopHolders(1);

        assertEquals(List.of(td.ACC_10), accounts);
    }

    @Test
    void getTotalAmountOnTopAccounts() {
        doReturn(2_000_000L).when(accountTable).getTotalAmountOnTopAccounts(1);

        long totalAmount = cachedTable.getTotalAmountOnTopAccounts(1);

        assertEquals(2_000_000L, totalAmount);
    }

    @Test
    void getTotalNumberOfAccounts() {
        doReturn(10L).when(accountTable).getTotalNumberOfAccounts();

        long accounts = cachedTable.getTotalNumberOfAccounts();

        assertEquals(10, accounts);
    }

    @Test
    void getRecentAccounts() {
        doReturn(List.of(td.ACC_0, td.ACC_10)).when(accountTable).getRecentAccounts(2);

        List<Account> accounts = cachedTable.getRecentAccounts(2);

        assertEquals(List.of(td.ACC_0, td.ACC_10), accounts);
    }
}