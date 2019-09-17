package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.app.*;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TxEventType;
import com.apollocurrency.aplwallet.apl.core.app.service.SecureStorageService;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollServiceImpl;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingApprovedResultTable;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingApprovalResult;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingVote;
import com.apollocurrency.aplwallet.apl.core.transaction.Payment;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.exchange.dao.*;
import com.apollocurrency.aplwallet.apl.exchange.model.*;
import com.apollocurrency.aplwallet.apl.exchange.transaction.DEX;
import com.apollocurrency.aplwallet.apl.exchange.transaction.DexTransferMoneyTransaction;
import com.apollocurrency.aplwallet.apl.testutil.WeldUtils;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.Time;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnableWeld
class DexServiceIntegrationTest {


    @WeldSetup
    WeldInitiator weld = WeldUtils.from(List.of(DexService.class), List.of(EthereumWalletService.class,
            DexOrderDao.class,
            DexOrderTable.class,
            TransactionProcessor.class,
            DexSmartContractService.class,
            SecureStorageService.class,
            DexContractTable.class,
            DexOrderTransactionCreator.class,
            TimeService.class,
            DexContractDao.class,
            Blockchain.class,
            PhasingPollServiceImpl.class,
            IDexMatcherInterface.class,
            DexTradeDao.class,
            PhasingApprovedResultTable.class,
            BlockchainConfig.class,
            BlockchainImpl.class)).build();
    @Inject
    DexService dexService;
    @Inject
    Event<Transaction> txEvent;
    @Inject
    PhasingPollServiceImpl phasingPollService;
    @Inject
    PhasingApprovedResultTable approvedResultTable;

    @Test
    void testTriggerPhasingTxReleaseEvent() {
        doReturn(List.of(new PhasingVote(null, 500, 1, 100, 20))).when(phasingPollService).getVotes(1);
        Transaction phasedTx = mock(Transaction.class);
        doReturn(1L).when(phasedTx).getId();
        doReturn(DEX.DEX_TRANSFER_MONEY_TRANSACTION).when(phasedTx).getType();

        txEvent.select(TxEventType.literal(TxEventType.RELEASE_PHASED_TRANSACTION)).fire(phasedTx);

        verify(approvedResultTable).insert(new PhasingApprovalResult(0, 1, 20));

    }

    @Test
    void testTriggerPhasingTxRejectedEvent() {
        Transaction phasedTx = mock(Transaction.class);

        txEvent.select(TxEventType.literal(TxEventType.REJECT_PHASED_TRANSACTION)).fire(phasedTx);

        verifyZeroInteractions(phasingPollService, approvedResultTable, phasedTx);
    }

    @Test
    void testTriggerPhasingReleasedTxEventForDifferentTxType() {
        Transaction phasedTx = mock(Transaction.class);
        doReturn(Payment.ORDINARY).when(phasedTx).getType();

        txEvent.select(TxEventType.literal(TxEventType.RELEASE_PHASED_TRANSACTION)).fire(phasedTx);

        verifyZeroInteractions(phasingPollService, approvedResultTable);
    }
}
