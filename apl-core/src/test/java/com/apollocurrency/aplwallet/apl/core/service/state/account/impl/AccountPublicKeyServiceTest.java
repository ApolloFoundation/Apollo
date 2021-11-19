/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.account.impl;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.PublicKeyDao;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.enterprise.event.Event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AccountPublicKeyServiceTest {
    private BlockChainInfoService blockChainInfoService = mock(BlockChainInfoService.class);
    private PublicKeyDao publicKeyDao = mock(TwoTablesPublicKeyDao.class);
    private Event event = mock(Event.class);

    private AccountPublicKeyService accountPublicKeyService;
    private AccountTestData testData;

    @BeforeEach
    void setUp() {
        testData = new AccountTestData();
        accountPublicKeyService = spy(new AccountPublicKeyServiceImpl(blockChainInfoService, publicKeyDao, event));
    }

    @Test
    void getCount() {
        int publicCount = 200000;
        int genesisCount = 300000;
        doReturn(publicCount).when(publicKeyDao).count();
        doReturn(genesisCount).when(publicKeyDao).genesisCount();
        assertEquals(publicCount + genesisCount, accountPublicKeyService.getCount());
    }

    @Test
    void getPublicKey() {
        long accountId = 2728325718715804811L;

        assertNull(accountPublicKeyService.getPublicKeyByteArray(accountId));

        PublicKey expectedPublicKey = new PublicKey(accountId, null, 1000);
        doReturn(expectedPublicKey).when(publicKeyDao).searchAll(anyLong());
        assertNull(accountPublicKeyService.getPublicKeyByteArray(accountId));

        expectedPublicKey = new PublicKey(accountId, testData.PUBLIC_KEY_STR.getBytes(), 1000);
        doReturn(expectedPublicKey).when(publicKeyDao).searchAll(anyLong());
        assertEquals(expectedPublicKey.getPublicKey(), accountPublicKeyService.getPublicKeyByteArray(accountId));
    }

    @Test
    void setOrVerify() {
        long accountId = 2728325718715804811L;
        PublicKey expectedPublicKey = new PublicKey(accountId, null, 1000);
        doReturn(expectedPublicKey).when(publicKeyDao).searchAll(anyLong());
        //set new key
        assertTrue(accountPublicKeyService.setOrVerifyPublicKey(accountId, testData.PUBLIC_KEY_STR.getBytes()));

        //verify
        expectedPublicKey = new PublicKey(accountId, testData.PUBLIC_KEY_STR.getBytes(), 1000);
        doReturn(expectedPublicKey).when(publicKeyDao).searchAll(anyLong());
        //true, the same keys
        assertTrue(accountPublicKeyService.setOrVerifyPublicKey(accountId, testData.PUBLIC_KEY_STR.getBytes()));
        //false, different keys
        assertFalse(accountPublicKeyService.setOrVerifyPublicKey(accountId, testData.PUBLIC_KEY_STR2.getBytes()));
    }

    @Test
    void testApply_newKey() {
        long accountId = 2728325718715804811L;
        PublicKey expectedPublicKey = new PublicKey(accountId, null, 1000);
        doReturn(expectedPublicKey).when(publicKeyDao).searchAll(anyLong());
        //publickKey == null
        accountPublicKeyService.apply(testData.ACC_1, testData.PUBLIC_KEY_STR.getBytes(), false);

        verify(event).fire(expectedPublicKey);
        verify(publicKeyDao, times(1)).insert(any(PublicKey.class));
        assertEquals(expectedPublicKey, testData.ACC_1.getPublicKey());
    }

    @Test
    void testApply() {
        long accountId = 2728325718715804811L;
        PublicKey expectedPublicKey = null;

        //check public keys
        expectedPublicKey = new PublicKey(accountId, testData.PUBLIC_KEY_STR.getBytes(), 1000);
        doReturn(expectedPublicKey).when(publicKeyDao).searchAll(anyLong());
        //key mismatch
        assertThrows(IllegalStateException.class, () -> accountPublicKeyService.apply(testData.ACC_1, testData.PUBLIC_KEY_STR2.getBytes(), false));
        verifyNoInteractions(event);
        //key match
        expectedPublicKey.setHeight(998);
        accountPublicKeyService.apply(testData.ACC_1, testData.PUBLIC_KEY_STR.getBytes(), false);
        verify(publicKeyDao, times(2)).searchAll(anyLong());
        verify(event).fire(expectedPublicKey);
        assertEquals(expectedPublicKey, testData.ACC_1.getPublicKey());
    }

}