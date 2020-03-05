/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.AccountEventType;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountPropertyTable;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountProperty;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.enterprise.event.Event;

import static com.apollocurrency.aplwallet.apl.core.account.observer.events.AccountEventBinding.literal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class AccountPropertyServiceTest {

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private AccountPropertyTable accountPropertyTable = mock(AccountPropertyTable.class);
    private Event accountEvent = mock(Event.class);
    private Event accountPropertyEvent = mock(Event.class);

    private AccountPropertyService accountPropertyService;
    private AccountTestData testData;

    @BeforeEach
    void setUp() {
        testData = new AccountTestData();
        accountPropertyService = spy(new AccountPropertyServiceImpl(
                blockchain,
                accountPropertyTable,
                accountEvent,
                accountPropertyEvent
        ));
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void setProperty_updateProperty() {
        String newValue = "NewUpdatedValue";

        Event firedEventAcc = mock(Event.class);
        Event firedEventProp = mock(Event.class);

        doReturn(firedEventAcc).when(accountEvent).select(literal(AccountEventType.SET_PROPERTY));
        doReturn(firedEventProp).when(accountPropertyEvent).select(literal(AccountEventType.SET_PROPERTY));
        String property = testData.ACC_PROP_4.getProperty();
        doReturn(testData.ACC_PROP_4).when(accountPropertyTable).getProperty(testData.ACC_1.getId(), property, testData.ACC_6.getId());
        accountPropertyService.setProperty(testData.ACC_1, null, testData.ACC_6, property, newValue);

        assertEquals(newValue, testData.ACC_PROP_4.getValue());

        verify(accountPropertyTable).insert(testData.ACC_PROP_4);
        verify(firedEventAcc).fire(testData.ACC_1);
        verify(firedEventProp).fire(testData.ACC_PROP_4);
    }

    @Test
    void setProperty_newProperty() {
        String newValue = "NewInsertedValue";
        long transactionId = 123412341234L;

        Event firedEventAcc = mock(Event.class);
        Event firedEventProp = mock(Event.class);

        doReturn(firedEventAcc).when(accountEvent).select(literal(AccountEventType.SET_PROPERTY));
        doReturn(firedEventProp).when(accountPropertyEvent).select(literal(AccountEventType.SET_PROPERTY));
        String property = testData.ACC_PROP_6.getProperty();
        doReturn(null).when(accountPropertyTable).getProperty(testData.ACC_1.getId(), property, testData.ACC_10.getId());
        AccountProperty expectedAccountProperty = new AccountProperty(transactionId, testData.ACC_1.getId(), testData.ACC_10.getId(), property, newValue, blockchain.getHeight());
        Transaction transaction = mock(TransactionImpl.class);
        doReturn(transactionId).when(transaction).getId();

        accountPropertyService.setProperty(testData.ACC_1, transaction, testData.ACC_10, property, newValue);

        verify(accountPropertyTable).insert(expectedAccountProperty);
        verify(firedEventAcc).fire(testData.ACC_1);
        verify(firedEventProp).fire(expectedAccountProperty);
    }

    @Test
    void deleteProperty() {
        Event firedEventAcc = mock(Event.class);
        Event firedEventProp = mock(Event.class);

        doReturn(firedEventAcc).when(accountEvent).select(literal(AccountEventType.DELETE_PROPERTY));
        doReturn(firedEventProp).when(accountPropertyEvent).select(literal(AccountEventType.DELETE_PROPERTY));
        doReturn(testData.ACC_PROP_4).when(accountPropertyTable).get(any(DbKey.class));
        accountPropertyService.deleteProperty(testData.ACC_1, testData.ACC_PROP_4.getId());

        verify(accountPropertyTable).deleteAtHeight(testData.ACC_PROP_4, blockchain.getHeight());
        verify(firedEventAcc).fire(testData.ACC_1);
        verify(firedEventProp).fire(testData.ACC_PROP_4);
    }

    @Test
    void deleteProperty_notExistProperty() {
        doReturn(null).when(accountPropertyTable).get(any(DbKey.class));
        accountPropertyService.deleteProperty(testData.ACC_1, testData.ACC_PROP_4.getId());

        verify(accountPropertyTable, never()).deleteAtHeight(any(AccountProperty.class), anyInt());
    }

    @Test
    void deleteProperty_withException() {
        doReturn(testData.ACC_PROP_1).when(accountPropertyTable).get(any(DbKey.class));

        assertThrows(RuntimeException.class, () -> accountPropertyService.deleteProperty(testData.ACC_1, testData.ACC_PROP_4.getId()));
    }
}