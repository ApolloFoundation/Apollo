/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.app.service.SecureStorageService;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollServiceImpl;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingApprovedResultTable;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexContractDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexContractTable;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderTable;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexTradeDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.MandatoryTransactionDao;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderType;
import com.apollocurrency.aplwallet.apl.exchange.model.UserEthDepositInfo;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class DexServiceTest {

    @Mock EthereumWalletService ethWalletService;
    @Mock
    DexOrderDao dexOrderDao;
    @Mock
    DexOrderTable dexOrderTable;
    @Mock TransactionProcessor transactionProcessor;
    @Mock DexSmartContractService dexSmartContractService;
    @Mock SecureStorageService secureStorageService;
    @Mock DexContractTable dexContractTable;
    @Mock
    DexOrderTransactionCreator dexOrderTransactionCreator;
    @Mock TimeService timeService;
    @Mock DexContractDao dexContractDao;
    @Mock Blockchain blockchain;
    @Mock PhasingPollServiceImpl phasingPollService;
    @Mock DexMatcherServiceImpl dexMatcherService;
    @Mock DexTradeDao dexTradeDao;
    @Mock
    PhasingApprovedResultTable approvedResultTable;
    @Mock
    MandatoryTransactionDao mandatoryTransactionDao;

    DexOrder order = new DexOrder(2L, 100L, "from-address", "to-address", OrderType.BUY, OrderStatus.OPEN, DexCurrencies.APL, 127_000_000L, DexCurrencies.ETH, BigDecimal.valueOf(0.0001), 500);
    ExchangeContract contract = new ExchangeContract(
            0L, 2L, 1L, 3L, 200L, 100L,
            ExchangeContractStatus.STEP_3, new byte[32], "123",
            "0x86d5bc08c2eba828a8e3588e25ad26a312ce77f6ecc02e3500ba05607f49c935",
            new byte[32], Constants.DEX_MIN_CONTRACT_TIME_WAITING_TO_REPLY, null, true);

    DexService dexService;

    @BeforeEach
    void setUp() {
        dexService = new DexService(ethWalletService, dexOrderDao, dexOrderTable, transactionProcessor, dexSmartContractService, secureStorageService, dexContractTable, dexOrderTransactionCreator, timeService, dexContractDao, blockchain, phasingPollService, dexMatcherService, dexTradeDao, approvedResultTable, mandatoryTransactionDao);
    }

    @Test
    void testNotEnoughConfirmationsForAplTransaction() {
        doReturn(60).when(blockchain).getHeight();
        doReturn(false).when(blockchain).hasTransaction(123, 30);

        boolean hasEnoughConfirmations = dexService.hasConfirmations(contract, order);

        assertFalse(hasEnoughConfirmations);
    }

    @Test
    void testHasEnoughConfirmationsForAplTransaction() {
        doReturn(60).when(blockchain).getHeight();
        doReturn(true).when(blockchain).hasTransaction(123, 30);

        boolean hasEnoughConfirmations = dexService.hasConfirmations(contract, order);

        assertTrue(hasEnoughConfirmations);
    }

    @Test
    void testNotEnoughConfirmationsForEthTransaction() {
        order.setType(OrderType.SELL);
        doReturn(9).when(ethWalletService).getNumberOfConfirmations(contract.getTransferTxId());

        boolean hasEnoughConfirmations = dexService.hasConfirmations(contract, order);

        assertFalse(hasEnoughConfirmations);
    }

    @Test
    void testHasEnoughConfirmationsForEthTransaction() {
        order.setType(OrderType.SELL);
        doReturn(10).when(ethWalletService).getNumberOfConfirmations(contract.getTransferTxId());

        boolean hasEnoughConfirmations = dexService.hasConfirmations(contract, order);

        assertTrue(hasEnoughConfirmations);
    }

    @Test
    void testHasConfirmationsForUnknownCurrency() {
        order.setPairCurrency(DexCurrencies.APL); //set apl here, because apl cannot represent paired currency
        order.setType(OrderType.SELL);

        assertThrows(IllegalArgumentException.class, () -> dexService.hasConfirmations(contract, order));
    }

    @Test
    void testHasFrozenMoneyForSellOrder() {
        order.setType(OrderType.SELL);

        boolean result = dexService.hasFrozenMoney(order);

        assertTrue(result);
    }
    @Test
    void testHasFrozenMoneyForBuyOrder() throws AplException.ExecutiveProcessException {
        List<UserEthDepositInfo> userDeposits = List.of(new UserEthDepositInfo(order.getId(), BigDecimal.valueOf(0.000126), 2L), new UserEthDepositInfo(1L, BigDecimal.valueOf(0.000127), 1L), new UserEthDepositInfo(order.getId(), BigDecimal.valueOf(0.000127), 1L));
        doReturn(userDeposits).when(dexSmartContractService).getUserFilledDeposits(order.getFromAddress());

        boolean result = dexService.hasFrozenMoney(order);

        assertTrue(result);
    }

    @Test
    void testHasFrozenMoneyForBuyOrderWithoutUserDeposits() throws AplException.ExecutiveProcessException {
        List<UserEthDepositInfo> userDeposits = List.of();
        doReturn(userDeposits).when(dexSmartContractService).getUserFilledDeposits(order.getFromAddress());

        boolean result = dexService.hasFrozenMoney(order);

        assertFalse(result);
    }

    @Test
    void testHasFrozenMoneyForBuyOrderWithException() throws AplException.ExecutiveProcessException {
        doThrow(new AplException.ExecutiveProcessException()).when(dexSmartContractService).getUserFilledDeposits(order.getFromAddress());

        boolean result = dexService.hasFrozenMoney(order);

        assertFalse(result);
    }

//    @Test
//    void closeOverdueContracts() {
//        Integer currentTime = 1000;
//        List<ExchangeContract> contracts = new ArrayList();
//
//        ExchangeContract exchangeContract = ExchangeContract.builder()
//                .orderId(1L)
//                .counterOrderId(2L)
//                .build();
//        contracts.add(exchangeContract);
//
//        DexOrder order = DexOrder.builder()
//                .finishTime(currentTime + 1)
//                .build();
//
//        DexOrder expiredOrder = DexOrder.builder()
//                .finishTime(currentTime - 1)
//                .type(OrderType.BUY)
//                .pairCurrency(DexCurrencies.ETH)
//                .build();
//
//
//        doReturn(contracts).when(dexContractDao).getOverdueContractsStep1and2(anyInt());
//        doReturn(order).when(dexOrderTable).getByTxId(1L);
//        doReturn(expiredOrder).when(dexOrderTable).getByTxId(2L);
//        doReturn(null).when(secureStorageService).getUserPassPhrase(2L);
//        doNothing().when(dexOrderTable).insert(any());
//
//
//        dexService.closeOverdueContracts(currentTime);
//    }
}