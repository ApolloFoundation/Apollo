/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.account.impl;

import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountInfoTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountInfo;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchUpdater;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountInfoService;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AccountInfoServiceTest {
    AccountInfoService accountInfoService;
    AccountTestData testData;
    private Blockchain blockchain = mock(BlockchainImpl.class);
    private AccountInfoTable accountInfoTable = mock(AccountInfoTable.class);
    private FullTextSearchUpdater fullTextSearchUpdater = mock(FullTextSearchUpdater.class);

    @BeforeEach
    void setUp() {
        testData = new AccountTestData();
        accountInfoService = spy(new AccountInfoServiceImpl(blockchain, accountInfoTable, fullTextSearchUpdater));
    }

    @Test
    void updateAccountInfo() {
        String newName = "Expected new name";
        String newDescription = "ExpectedNewDescription";

        doReturn(testData.ACC_INFO_0).when(accountInfoTable).get(any());
        accountInfoService.updateAccountInfo(testData.ACC_0, newName, newDescription);
        verify(accountInfoService).update(testData.ACC_INFO_0);
        assertEquals(newName, testData.ACC_INFO_0.getName());
        assertEquals(newDescription, testData.ACC_INFO_0.getDescription());
    }

    @Test
    void updateAccountInfo_newInfo() {
        String newName = "Expected new name";
        String newDescription = "ExpectedNewDescription";
        AccountInfo expectedAccountInfo = new AccountInfo(
            testData.ACC_1.getId(), newName, newDescription, blockchain.getHeight());

        doReturn(null).when(accountInfoTable).get(any());
        accountInfoService.updateAccountInfo(testData.ACC_1, newName, newDescription);
        verify(accountInfoService).update(expectedAccountInfo);
    }

    @Test
    void testUpdate_as_insert() {
        AccountInfo newInfo = new AccountInfo(
            testData.newInfo.getAccountId(), testData.newInfo.getName(),
            testData.newInfo.getDescription(), testData.INFO_BLOCKCHAIN_HEIGHT);
        accountInfoService.update(newInfo);
        verify(accountInfoTable, times(1)).insert(newInfo);
        verify(accountInfoTable, never()).deleteAtHeight(any(AccountInfo.class), anyInt());
    }

    @Test
    void testUpdate_as_delete() {
        AccountInfo deletedAccountInfo = new AccountInfo(
            testData.ACC_1.getId(), null, null, blockchain.getHeight());
        accountInfoService.update(deletedAccountInfo);
        verify(accountInfoTable, times(1)).deleteAtHeight(deletedAccountInfo, blockchain.getHeight());
        verify(accountInfoTable, never()).insert(any(AccountInfo.class));
    }
}