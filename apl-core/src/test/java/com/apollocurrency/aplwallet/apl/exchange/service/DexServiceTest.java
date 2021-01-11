/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingApprovedResultTable;
import com.apollocurrency.aplwallet.apl.core.model.dex.DexOrder;
import com.apollocurrency.aplwallet.apl.core.model.dex.DexOrderWithFreezing;
import com.apollocurrency.aplwallet.apl.core.model.dex.ExchangeContract;
import com.apollocurrency.aplwallet.apl.core.service.appdata.SecureStorageService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.impl.PhasingPollServiceImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionSerializer;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionSerializerImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.dex.config.DexConfig;
import com.apollocurrency.aplwallet.apl.dex.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.DBSortOrder;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.DexOrderDBRequest;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.DexOrderSortBy;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.ExchangeContractStatus;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.OrderFreezing;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.OrderStatus;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.OrderType;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexContractDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexContractTable;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderTable;
import com.apollocurrency.aplwallet.apl.exchange.dao.MandatoryTransactionDao;
import com.apollocurrency.aplwallet.vault.KeyStoreService;
import com.apollocurrency.aplwallet.vault.service.auth.Account2FAService;
import com.google.common.cache.LoadingCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class DexServiceTest {

    @Mock
    EthereumWalletService ethWalletService;
    @Mock
    DexOrderDao dexOrderDao;
    @Mock
    DexOrderTable dexOrderTable;
    @Mock
    TransactionProcessor transactionProcessor;
    @Mock
    DexSmartContractService dexSmartContractService;
    @Mock
    SecureStorageService secureStorageService;
    @Mock
    DexContractTable dexContractTable;
    @Mock
    DexOrderTransactionCreator dexOrderTransactionCreator;
    @Mock
    TimeService timeService;
    @Mock
    DexContractDao dexContractDao;
    @Mock
    Blockchain blockchain;
    @Mock
    PhasingPollServiceImpl phasingPollService;
    @Mock
    DexMatcherServiceImpl dexMatcherService;
    @Mock
    PhasingApprovedResultTable approvedResultTable;
    @Mock
    MandatoryTransactionDao mandatoryTransactionDao;
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    LoadingCache<Long, OrderFreezing> cache;
    @Mock
    DexConfig dexConfig;
    @Mock
    AccountService accountService;
    @Mock
    KeyStoreService keyStoreService;
    @Mock
    Account2FAService account2FAService;

    DexOrder order = new DexOrder(2L, 100L, "from-address", "to-address", OrderType.BUY, OrderStatus.OPEN, DexCurrency.APL, 127_000_000L, DexCurrency.ETH, BigDecimal.valueOf(0.0001), 500);
    DexOrder order1 = new DexOrder(1L, 2L, OrderType.BUY, 100L, DexCurrency.APL, 10000L, DexCurrency.PAX, BigDecimal.ONE, 90, OrderStatus.OPEN, 259, "", "");
    DexOrder order2 = new DexOrder(2L, 4L, OrderType.SELL, 200L, DexCurrency.APL, 50000L, DexCurrency.ETH, BigDecimal.TEN, 290, OrderStatus.WAITING_APPROVAL, 380, "", "");
    DexOrder order3 = new DexOrder(3L, 6L, OrderType.BUY, 200L, DexCurrency.APL, 100000L, DexCurrency.ETH, BigDecimal.TEN, 290, OrderStatus.WAITING_APPROVAL, 380, "", "");
    DexOrder order4 = new DexOrder(4L, 8L, OrderType.BUY, 100L, DexCurrency.APL, 20000L, DexCurrency.PAX, BigDecimal.valueOf(2.2), 500, OrderStatus.PENDING, 381, "", "");
    ExchangeContract contract = new ExchangeContract(
        0L, 2L, 1L, 3L, 200L, 100L,
        ExchangeContractStatus.STEP_3, new byte[32], "123",
        "0x86d5bc08c2eba828a8e3588e25ad26a312ce77f6ecc02e3500ba05607f49c935",
        new byte[32], 100, null, true);

    DexService dexService;

    @BeforeEach
    void setUp() {
        TransactionSerializer serializer = new TransactionSerializerImpl(mock(PrunableLoadingService.class));
        dexService = new DexService(ethWalletService, dexOrderDao, dexOrderTable, transactionProcessor, dexSmartContractService, secureStorageService,
            dexContractTable, dexOrderTransactionCreator, timeService, dexContractDao, blockchain, phasingPollService, dexMatcherService,
            approvedResultTable, mandatoryTransactionDao, serializer, accountService, blockchainConfig, cache, dexConfig, keyStoreService, account2FAService);
    }

    @Test
    void testNotEnoughConfirmationsForAplTransaction() {
        doReturn(60).when(blockchain).getHeight();
        doReturn(false).when(blockchain).hasTransaction(123, 30);
        doReturn(30).when(dexConfig).getAplConfirmations();

        boolean hasEnoughConfirmations = dexService.hasConfirmations(contract, order);

        assertFalse(hasEnoughConfirmations);
    }

    @Test
    void testHasEnoughConfirmationsForAplTransaction() {
        doReturn(60).when(blockchain).getHeight();
        doReturn(true).when(blockchain).hasTransaction(123, 30);
        doReturn(30).when(dexConfig).getAplConfirmations();

        boolean hasEnoughConfirmations = dexService.hasConfirmations(contract, order);

        assertTrue(hasEnoughConfirmations);
    }

    @Test
    void testNotEnoughConfirmationsForEthTransaction() {
        order.setType(OrderType.SELL);
        doReturn(9).when(ethWalletService).getNumberOfConfirmations(contract.getTransferTxId());
        doReturn(10).when(dexConfig).getEthConfirmations();

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
        order.setPairCurrency(DexCurrency.APL); //set apl here, because apl cannot represent paired currency
        order.setType(OrderType.SELL);

        assertThrows(IllegalArgumentException.class, () -> dexService.hasConfirmations(contract, order));
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

    @Test
    void testGetOrdersWithoutHasFrozenMoneyParameter() {
        DexOrder order1 = new DexOrder(1L, 2L, OrderType.BUY, 100L, DexCurrency.APL, 10000L, DexCurrency.PAX, BigDecimal.ONE, 90, OrderStatus.OPEN, 259, "", "");
        DexOrder order2 = new DexOrder(2L, 4L, OrderType.SELL, 200L, DexCurrency.APL, 50000L, DexCurrency.ETH, BigDecimal.TEN, 290, OrderStatus.WAITING_APPROVAL, 380, "", "");
        DexOrderDBRequest request = DexOrderDBRequest.builder().limit(2).sortBy(DexOrderSortBy.PAIR_RATE).sortOrder(DBSortOrder.DESC).build();
        doReturn(List.of(order1, order2)).when(dexOrderDao).getOrders(request, DexOrderSortBy.PAIR_RATE, DBSortOrder.DESC);
        doReturn(new OrderFreezing(2L, false)).when(cache).getUnchecked(2L);

        List<DexOrderWithFreezing> ordersWithFreezing = dexService.getOrdersWithFreezing(request);

        assertEquals(List.of(new DexOrderWithFreezing(order1, false), new DexOrderWithFreezing(order2, true)), ordersWithFreezing);
    }

    @Test
    void testGetOrdersWithFrozenMoney() {
        DexOrderDBRequest request1 = DexOrderDBRequest.builder().offset(1).limit(2).sortBy(DexOrderSortBy.PAIR_RATE).sortOrder(DBSortOrder.DESC).hasFrozenMoney(true).build();
        doReturn(List.of(order1, order2)).when(dexOrderDao).getOrders(request1, DexOrderSortBy.PAIR_RATE, DBSortOrder.DESC);
        DexOrderDBRequest request2 = DexOrderDBRequest.builder().offset(3).limit(2).sortBy(DexOrderSortBy.PAIR_RATE).sortOrder(DBSortOrder.DESC).hasFrozenMoney(true).build();
        doReturn(List.of(order3, order4)).when(dexOrderDao).getOrders(request2, DexOrderSortBy.PAIR_RATE, DBSortOrder.DESC);

        doReturn(new OrderFreezing(2L, false)).when(cache).getUnchecked(2L);
        doReturn(new OrderFreezing(6L, true)).when(cache).getUnchecked(6L);
        doReturn(new OrderFreezing(8L, false)).when(cache).getUnchecked(8L);

        List<DexOrderWithFreezing> ordersWithFreezing = dexService.getOrdersWithFreezing(request1);

        assertEquals(List.of(new DexOrderWithFreezing(order2, true), new DexOrderWithFreezing(order3, true)), ordersWithFreezing);
    }

    @Test
    void testGetOrdersWithoutFrozenMoney() {
        DexOrderDBRequest request1 = DexOrderDBRequest.builder().offset(1).limit(2).sortBy(DexOrderSortBy.PAIR_RATE).sortOrder(DBSortOrder.DESC).hasFrozenMoney(false).build();
        doReturn(List.of(order1, order2)).when(dexOrderDao).getOrders(request1, DexOrderSortBy.PAIR_RATE, DBSortOrder.DESC);
        DexOrderDBRequest request2 = DexOrderDBRequest.builder().offset(3).limit(2).sortBy(DexOrderSortBy.PAIR_RATE).sortOrder(DBSortOrder.DESC).hasFrozenMoney(false).build();
        doReturn(List.of(order3, order4)).when(dexOrderDao).getOrders(request2, DexOrderSortBy.PAIR_RATE, DBSortOrder.DESC);
        DexOrderDBRequest request3 = DexOrderDBRequest.builder().offset(5).limit(2).sortBy(DexOrderSortBy.PAIR_RATE).sortOrder(DBSortOrder.DESC).hasFrozenMoney(false).build();
        doReturn(List.of(order)).when(dexOrderDao).getOrders(request3, DexOrderSortBy.PAIR_RATE, DBSortOrder.DESC);

        doReturn(new OrderFreezing(2L, true)).when(cache).getUnchecked(2L);
        doReturn(new OrderFreezing(6L, true)).when(cache).getUnchecked(6L);
        doReturn(new OrderFreezing(8L, false)).when(cache).getUnchecked(8L);

        List<DexOrderWithFreezing> ordersWithFreezing = dexService.getOrdersWithFreezing(request1);

        assertEquals(List.of(new DexOrderWithFreezing(order4, false)), ordersWithFreezing);
    }

    @Test
    void testGetOrdersWithoutOffset() {
        DexOrderDBRequest request1 = DexOrderDBRequest.builder().limit(2).sortBy(DexOrderSortBy.PAIR_RATE).sortOrder(DBSortOrder.DESC).hasFrozenMoney(false).build();
        doReturn(List.of(order1, order2)).when(dexOrderDao).getOrders(request1, DexOrderSortBy.PAIR_RATE, DBSortOrder.DESC);
        DexOrderDBRequest request2 = DexOrderDBRequest.builder().offset(2).limit(2).sortBy(DexOrderSortBy.PAIR_RATE).sortOrder(DBSortOrder.DESC).hasFrozenMoney(false).build();
        doReturn(List.of()).when(dexOrderDao).getOrders(request2, DexOrderSortBy.PAIR_RATE, DBSortOrder.DESC);

        doReturn(new OrderFreezing(2L, false)).when(cache).getUnchecked(2L);

        List<DexOrderWithFreezing> ordersWithFreezing = dexService.getOrdersWithFreezing(request1);

        assertEquals(List.of(new DexOrderWithFreezing(order1, false)), ordersWithFreezing);
    }

    @Test
    void testGetAccountOrderContracts() {
        List<ExchangeContract> expected = List.of(this.contract);
        doReturn(expected).when(dexContractDao).getAllForAccountOrder(this.contract.getSender(), this.contract.getOrderId(), 0, 3);

        List<ExchangeContract> result = dexService.getContractsByAccountOrderFromStatus(this.contract.getSender(), this.contract.getOrderId(), (byte) 0);

        assertSame(expected, result);
    }

    @Test
    void testGetAccountOrderContractWithStatus() {
        List<ExchangeContract> expected = List.of(this.contract);
        doReturn(expected).when(dexContractDao).getAllForAccountOrder(this.contract.getSender(), this.contract.getOrderId(), 1, 1);

        List<ExchangeContract> result = dexService.getContractsByAccountOrderWithStatus(this.contract.getSender(), this.contract.getOrderId(), (byte) 1);

        assertSame(expected, result);
    }

    @Test
    void testGetAccountOrderVersionedContracts() {
        List<ExchangeContract> expected = List.of(this.contract);
        doReturn(expected).when(dexContractDao).getAllVersionedForAccountOrder(this.contract.getSender(), this.contract.getOrderId(), 0, 3);

        List<ExchangeContract> result = dexService.getVersionedContractsByAccountOrder(this.contract.getSender(), this.contract.getOrderId());

        assertSame(expected, result);
    }
}