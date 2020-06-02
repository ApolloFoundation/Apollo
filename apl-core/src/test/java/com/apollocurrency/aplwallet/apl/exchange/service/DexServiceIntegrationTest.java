/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.service.operation.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.operation.account.impl.AccountServiceImpl;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TxEventType;
import com.apollocurrency.aplwallet.apl.core.service.appdata.SecureStorageService;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.service.operation.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.dao.operation.phasing.PhasingApprovedResultTable;
import com.apollocurrency.aplwallet.apl.core.entity.operation.phasing.PhasingApprovalResult;
import com.apollocurrency.aplwallet.apl.core.entity.operation.phasing.PhasingVote;
import com.apollocurrency.aplwallet.apl.core.transaction.Payment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexControlOfFrozenMoneyAttachment;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.exchange.DexConfig;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexContractDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexContractTable;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderTable;
import com.apollocurrency.aplwallet.apl.exchange.dao.MandatoryTransactionDao;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderFreezing;
import com.apollocurrency.aplwallet.apl.exchange.transaction.DEX;
import com.apollocurrency.aplwallet.apl.testutil.WeldUtils;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;

import javax.enterprise.event.Event;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@EnableWeld
class DexServiceIntegrationTest {

    @WeldSetup
    WeldInitiator weld = WeldUtils.from(List.of(DexService.class, CacheProducer.class), List.of(EthereumWalletService.class,
        DexOrderDao.class,
        DexOrderTable.class,
        TransactionProcessor.class,
        DexSmartContractService.class,
        SecureStorageService.class,
        DexContractTable.class,
        MandatoryTransactionDao.class,
        DexOrderTransactionCreator.class,
        TimeService.class,
        DexContractDao.class,
        Blockchain.class,
        IDexMatcherInterface.class,
        PhasingApprovedResultTable.class,
        BlockchainConfig.class,
        DexConfig.class,
        BlockchainImpl.class))
        .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
        .addBeans(MockBean.of(mock(AccountService.class), AccountService.class, AccountServiceImpl.class))
        .build();
    @Inject
    DexService dexService;
    @Inject
    Event<Transaction> txEvent;
    @Inject
    PhasingPollService phasingPollService;
    @Inject
    PhasingApprovedResultTable approvedResultTable;

    @Test
    void testTriggerPhasingTxReleaseEvent() {
        doReturn(List.of(new PhasingVote(null, 500, 1, 100, 20), new PhasingVote(null, 499, 1, 200, 30))).when(phasingPollService).getVotes(1);
        Transaction phasedTx = mock(Transaction.class);
        doReturn(1L).when(phasedTx).getId();
        doReturn(DEX.DEX_TRANSFER_MONEY_TRANSACTION).when(phasedTx).getType();
        DexControlOfFrozenMoneyAttachment attachment = new DexControlOfFrozenMoneyAttachment(100L, 200L);
        doReturn(attachment).when(phasedTx).getAttachment();
        txEvent.select(TxEventType.literal(TxEventType.RELEASE_PHASED_TRANSACTION)).fire(phasedTx);

        verify(approvedResultTable).insert(new PhasingApprovalResult(0, 1, 20));

    }

    @Test
    void testTriggerPhasingForDifferentEvent() {
        Transaction phasedTx = mock(Transaction.class);

        txEvent.select(TxEventType.literal(TxEventType.REMOVED_UNCONFIRMED_TRANSACTIONS)).fire(phasedTx);

        verifyZeroInteractions(phasingPollService, approvedResultTable, phasedTx);
    }

    @Test
    void testTriggerPhasingReleasedTxEventForDifferentTxType() {
        Transaction phasedTx = mock(Transaction.class);
        doReturn(Payment.ORDINARY).when(phasedTx).getType();

        txEvent.select(TxEventType.literal(TxEventType.RELEASE_PHASED_TRANSACTION)).fire(phasedTx);

        verifyZeroInteractions(phasingPollService, approvedResultTable);
    }

    @Singleton
    static class CacheProducer {
        @Produces
        private LoadingCache<Long, OrderFreezing> createCache() {
            return CacheBuilder.newBuilder().build(CacheLoader.from(ord -> new OrderFreezing(1, true)));
        }

    }
}
