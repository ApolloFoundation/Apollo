package com.apollocurrency.aplwallet.apl.exchange.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.app.service.SecureStorageService;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollServiceImpl;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexContractDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexContractTable;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOfferDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOfferTable;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexTradeDao;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

@ExtendWith(MockitoExtension.class)
class DexServiceTest {

    @Mock EthereumWalletService ethWalletService;
    @Mock DexOfferDao dexOfferDao;
    @Mock DexOfferTable dexOfferTable;
    @Mock TransactionProcessor transactionProcessor;
    @Mock DexSmartContractService dexSmartContractService;
    @Mock SecureStorageService secureStorageService;
    @Mock DexContractTable dexContractTable;
    @Mock DexOfferTransactionCreator dexOfferTransactionCreator;
    @Mock TimeService timeService;
    @Mock DexContractDao dexContractDao;
    @Mock Blockchain blockchain;
    @Mock PhasingPollServiceImpl phasingPollService;
    @Mock DexMatcherServiceImpl dexMatcherService;
    @Mock DexTradeDao dexTradeDao;

    DexOffer offer = new DexOffer(1L, 2L, 100L, "from-address", "to-address", OfferType.BUY, OfferStatus.OPEN, DexCurrencies.APL, 100_000_000L, DexCurrencies.ETH, BigDecimal.valueOf(0.0001), 500);
    ExchangeContract contract = new ExchangeContract(2L, 1L, 3L, 200L, 100L, ExchangeContractStatus.STEP_3, new byte[32], "123", "0x86d5bc08c2eba828a8e3588e25ad26a312ce77f6ecc02e3500ba05607f49c935", new byte[32]);

    DexService dexService;

    @BeforeEach
    void setUp() {
        dexService = new DexService(ethWalletService,dexOfferDao, dexOfferTable, transactionProcessor, dexSmartContractService, secureStorageService, dexContractTable, dexOfferTransactionCreator, timeService, dexContractDao, blockchain, phasingPollService, dexMatcherService, dexTradeDao);
    }

    @Test
    void testNotEnoughConfirmationsForAplTransaction() {
        doReturn(1000).when(blockchain).getHeight();
        doReturn(false).when(blockchain).hasTransaction(123, 970);

        boolean hasEnoughConfirmations = dexService.hasConfirmations(contract, offer);

        assertFalse(hasEnoughConfirmations);
    }

    @Test
    void testHasEnoughConfirmationsForAplTransaction() {
        doReturn(100).when(blockchain).getHeight();
        doReturn(true).when(blockchain).hasTransaction(123, 70);

        boolean hasEnoughConfirmations = dexService.hasConfirmations(contract, offer);

        assertTrue(hasEnoughConfirmations);
    }

    @Test
    void testNotEnoughConfirmationsForEthTransaction() {
        offer.setType(OfferType.SELL);
        doReturn(9).when(ethWalletService).getNumberOfConfirmations(contract.getTransferTxId());

        boolean hasEnoughConfirmations = dexService.hasConfirmations(contract, offer);

        assertFalse(hasEnoughConfirmations);
    }

    @Test
    void testHasEnoughConfirmationsForEthTransaction() {
        offer.setType(OfferType.SELL);
        doReturn(10).when(ethWalletService).getNumberOfConfirmations(contract.getTransferTxId());

        boolean hasEnoughConfirmations = dexService.hasConfirmations(contract, offer);

        assertTrue(hasEnoughConfirmations);
    }

    @Test
    void testHasConfirmationsForUnknownCurrency() {
        offer.setPairCurrency(DexCurrencies.APL); //set apl here, because apl cannot represent paired currency
        offer.setType(OfferType.SELL);

        assertThrows(IllegalArgumentException.class, () -> dexService.hasConfirmations(contract, offer));
    }
}