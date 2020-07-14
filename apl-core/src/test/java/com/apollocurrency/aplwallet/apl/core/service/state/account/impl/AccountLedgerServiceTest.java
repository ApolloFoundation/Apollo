/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.account.impl;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountLedgerEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountLedgerEventType;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountLedgerTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEntry;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountLedgerService;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.enterprise.event.Event;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AccountLedgerServiceTest {

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessorImpl.class);
    private AccountLedgerTable accountLedgerTable = mock(AccountLedgerTable.class);
    private PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    private Event ledgerEvent = mock(Event.class);

    private AccountLedgerService accountLedgerService;
    private AccountTestData testData;

    private AccountLedgerService createLedgerServiceInstance() {
        return new AccountLedgerServiceImpl(
            accountLedgerTable,
            blockchain,
            blockchainProcessor,
            propertiesHolder,
            blockchainConfig,
            ledgerEvent
        );
    }

    @BeforeEach
    void setUp() {
        testData = new AccountTestData();
        doReturn(30000).when(accountLedgerTable).getTrimKeep();
        doReturn(true).when(blockchainProcessor).isProcessingBlock();
        doReturn(List.of("*")).when(propertiesHolder).getStringListProperty("apl.ledgerAccounts");
        accountLedgerService = spy(createLedgerServiceInstance());
    }

    @Test
    void mustLogEntry() {
        boolean result;

        //Check Log Account
        doReturn(Collections.EMPTY_LIST).when(propertiesHolder).getStringListProperty("apl.ledgerAccounts");
        result = createLedgerServiceInstance().mustLogEntry(testData.ACC_1.getId(), false);
        assertEquals(false, result);

        doReturn(List.of("*")).when(propertiesHolder).getStringListProperty("apl.ledgerAccounts");

        //Check Log Unconfirmed
        doReturn(0).when(propertiesHolder).getIntProperty("apl.ledgerLogUnconfirmed", 1);
        result = createLedgerServiceInstance().mustLogEntry(testData.ACC_1.getId(), true);
        assertEquals(false, result);

        doReturn(2).when(propertiesHolder).getIntProperty("apl.ledgerLogUnconfirmed", 1);
        result = createLedgerServiceInstance().mustLogEntry(testData.ACC_1.getId(), false);
        assertEquals(false, result);

        doReturn(1).when(propertiesHolder).getIntProperty("apl.ledgerLogUnconfirmed", 1);
        //Check heights
        doReturn(70_000).when(blockchain).getHeight();
        doReturn(100_000L).when(blockchainConfig).getLastKnownBlock();
        result = createLedgerServiceInstance().mustLogEntry(testData.ACC_1.getId(), false);
        assertEquals(false, result);

        //Check when scanning
        doReturn(100_000).when(blockchain).getHeight();
        doReturn(150_000).when(blockchainProcessor).getInitialScanHeight();
        doReturn(true).when(blockchainProcessor).isScanning();
        result = createLedgerServiceInstance().mustLogEntry(testData.ACC_1.getId(), false);
        assertEquals(false, result);

        doReturn(false).when(blockchainProcessor).isScanning();
        result = createLedgerServiceInstance().mustLogEntry(testData.ACC_1.getId(), false);
        assertEquals(true, result);
    }

    @Test
    void logEntry() {
        doReturn(true).when(accountLedgerTable).isInTransaction();
        accountLedgerService.clearEntries();
        List<LedgerEntry> pendingEntries = ((AccountLedgerServiceImpl) accountLedgerService).getPendingEntries();
        //2,5,6  - have the same accountId
        pendingEntries.addAll(testData.PENDING_LEDGERS);
        final LedgerEntry ledgerEntry = testData.ACC_LEDGER_ADD;
        //pendingEntries.remove(ledgerEntry);
        long adjustedBalance = testData.ACC_LEDGER_2.getBalance() - testData.ACC_LEDGER_2.getChange();
        for (LedgerEntry existingEntry : testData.SAME_ACC_LEDGERS) {
            adjustedBalance += existingEntry.getChange();
        }

        accountLedgerService.logEntry(ledgerEntry);

        assertEquals(adjustedBalance, testData.ACC_LEDGER_6.getBalance());
        assertTrue(pendingEntries.contains(ledgerEntry));
    }

    @Test
    void commitEntries() {
        int pendingCount = testData.PENDING_LEDGERS.size();
        List<LedgerEntry> pendingEntries = ((AccountLedgerServiceImpl) accountLedgerService).getPendingEntries();
        pendingEntries.addAll(testData.PENDING_LEDGERS);
        Event firedEvent = mock(Event.class);
        doReturn(firedEvent).when(ledgerEvent).select(AccountLedgerEventBinding.literal(AccountLedgerEventType.ADD_ENTRY));

        accountLedgerService.commitEntries();

        verify(accountLedgerTable, times(pendingCount)).insert(any(LedgerEntry.class));
        verify(firedEvent, times(pendingCount)).fire(any(LedgerEntry.class));
        assertEquals(0, pendingEntries.size());
    }

    @Test
    void clearEntries() {
        List<LedgerEntry> pendingEntries = ((AccountLedgerServiceImpl) accountLedgerService).getPendingEntries();
        pendingEntries.clear();
        pendingEntries.addAll(testData.PENDING_LEDGERS);
        assertTrue(pendingEntries.size() > 0);
        accountLedgerService.clearEntries();
        assertEquals(0, pendingEntries.size());
    }

}