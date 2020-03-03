package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.DexOrderDto;
import com.apollocurrency.aplwallet.api.dto.TradingDataOutputDTO;
import com.apollocurrency.aplwallet.api.response.Account2FAResponse;
import com.apollocurrency.aplwallet.api.response.CreateDexOrderResponse;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrency.aplwallet.api.response.EthGasInfoResponse;
import com.apollocurrency.aplwallet.api.response.WithdrawResponse;
import com.apollocurrrency.aplwallet.inttest.helper.DexPreconditionExtension;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.model.TestBase;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseNew;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseOld;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Epic;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extensions;
import org.junit.jupiter.api.parallel.Execution;

import java.util.List;
import java.util.Objects;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.parallel.ExecutionMode;


import java.lang.annotation.ElementType;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

@DisplayName("Dex")
@Epic(value = "Dex")
@ExtendWith(DexPreconditionExtension.class)
@Execution(CONCURRENT)
public class TestDex extends TestBaseNew {


    private static Wallet vault1 = new Wallet("APL-D8L6-UJ22-PUK9-6EYMD", "1", true, "0xd54a7a3eff64b467f01f0640b201977e8d017c97", "5030464519701633604");
    private static Wallet vault2 = new Wallet("APL-UB87-LVCF-M8LK-HC9U6", "1", true, "0x19826b8d344582a5e4610b300cb16de86a0d9f89", "17467469088293725381");



    @BeforeAll
    public static void initAll(){
        TestBase.initAll();
        CreateTransactionResponse transactionResponse;
        ClassLoader classLoader = TestBase.class.getClassLoader();
        String secretFilePathVault1 = Objects.requireNonNull(classLoader.getResource(vault1.getUser()).getPath());
        String secretFilePathVault2 = Objects.requireNonNull(classLoader.getResource(vault2.getUser()).getPath());
        try {
            importSecretFileSetUp(secretFilePathVault1, vault1.getPass());
            importSecretFileSetUp(secretFilePathVault2, vault2.getPass());
        } catch (Exception ex) {
           fail("Precondition FAILED: " + ex.getMessage(), ex);
        }
        if (getBalanceSetUP(vault1).getBalanceATM() < 90000000000000L) {
            log.info("Send money on: " + vault1);

            transactionResponse = sendMoneySetUp(TestConfiguration.getTestConfiguration().getGenesisWallet(),
                vault1.getUser(), 1000000);
            verifyTransactionInBlockSetUp(transactionResponse.getTransaction());

            log.info("Verify account: " + vault1);
            transactionResponse = sendMoneySetUp(vault1,
                vault1.getUser(), 10);
            verifyTransactionInBlockSetUp(transactionResponse.getTransaction());
        }
        if (getBalanceSetUP(vault2).getBalanceATM() < 90000000000000L) {
            log.info("Send money on: " + vault2);

            transactionResponse = sendMoneySetUp(TestConfiguration.getTestConfiguration().getGenesisWallet(),
                vault2.getUser(), 1000000);
            verifyTransactionInBlockSetUp(transactionResponse.getTransaction());

            log.info("Verify account: " + vault2);
            transactionResponse = sendMoneySetUp(vault2,
                vault2.getUser(), 10);
            verifyTransactionInBlockSetUp(transactionResponse.getTransaction());
        }


    @DisplayName("Get trading history (closed orders) for certain account with param")
    @Test
    @Execution(SAME_THREAD)
    public void getTradeHistory() {
        List<DexOrderDto> ordersVault1 = getDexHistory(vault1.getAccountId(), true, true);
        assertNotNull(ordersVault1);
        List<DexOrderDto> ordersVault2 = getDexHistory(vault2.getAccountId(), true, true);
        assertNotNull(ordersVault2);
    }

    @DisplayName("Get trading history (closed orders) for certain account")
    @Test
    @Execution(SAME_THREAD)
    public void getAllTradeHistoryByAccount() {
        List<DexOrderDto> ordersVault1 = getDexHistory(vault1.getAccountId());
        assertNotNull(ordersVault1);
        List<DexOrderDto> ordersVault2 = getDexHistory(vault2.getAccountId());
        assertNotNull(ordersVault2);
    }

    @DisplayName("Get gas prices for different tx speed")
    @Test
    @Execution(SAME_THREAD)
    public void getEthGasPrice() {
        EthGasInfoResponse gasPrice = getEthGasInfo();
        assertTrue(Float.valueOf(gasPrice.getFast()) >= Float.valueOf(gasPrice.getAverage()));
        assertTrue(Float.valueOf(gasPrice.getAverage()) >= Float.valueOf(gasPrice.getSafeLow()));
        assertTrue(Float.valueOf(gasPrice.getSafeLow()) > 0);
    }

    @DisplayName("Obtaining ETH trading information for the given period (10 days) with certain resolution")
    @Test
    @Execution(SAME_THREAD)
    //@ParameterizedTest
    //@ValueSource(strings = {"D", "15", "60", "240"})
    public void getDexTradeInfoETH() {
        TradingDataOutputDTO dexTrades = getDexTradeInfo(true, "D");
        assertNotNull(dexTrades.getL(), "response is incorrect");
        assertNotNull(dexTrades.getC(), "response is incorrect");
        assertNotNull(dexTrades.getH(), "response is incorrect");
        assertNotNull(dexTrades.getO(), "response is incorrect");
        assertNotNull(dexTrades.getS(), "response is incorrect");
        assertNotNull(dexTrades.getT(), "response is incorrect");
        assertNotNull(dexTrades.getV(), "response is incorrect");
    }

    @DisplayName("Obtaining PAX trading information for the given period (10 days) with certain resolution")
    @Test
    @Execution(SAME_THREAD)
    public void getDexTradeInfoPAX() {
        TradingDataOutputDTO dexTrades = getDexTradeInfo(false, "15");
        assertNotNull(dexTrades.getL(), "response is incorrect");
        assertNotNull(dexTrades.getC(), "response is incorrect");
        assertNotNull(dexTrades.getH(), "response is incorrect");
        assertNotNull(dexTrades.getO(), "response is incorrect");
        assertNotNull(dexTrades.getS(), "response is incorrect");
        assertNotNull(dexTrades.getT(), "response is incorrect");
        assertNotNull(dexTrades.getV(), "response is incorrect");
    }

    @DisplayName("Create 4 types of orders and cancel them")
    @Test
    @Execution(SAME_THREAD)
    public void dexOrders() {
        log.info("Creating SELL Dex Order (ETH)");
        CreateDexOrderResponse sellOrderEth = createDexOrder("40000", "1000", vault1, false, true);
        assertNotNull(sellOrderEth, "RESPONSE is not correct/dex offer wasn't created");
        assertNotNull(sellOrderEth.getOrder().getId());
        verifyTransactionInBlock(sellOrderEth.getOrder().getId());

        log.info("Creating SELL Dex Order (PAX)");
        CreateDexOrderResponse sellOrderPax = createDexOrder("40000", "1000", vault1, false, false);
        assertNotNull(sellOrderPax, "RESPONSE is not correct/dex offer wasn't created");
        verifyTransactionInBlock(sellOrderPax.getOrder().getId());

        log.info("Creating BUY Dex Order (ETH)");
        CreateDexOrderResponse buyOrderEth = createDexOrder("15000", "1000", vault1, true, true);
        assertNotNull(buyOrderEth,  "RESPONSE is not correct/dex offer wasn't created");
        assertNotNull(buyOrderEth.getFrozenTx(), "FrozenTx isn't present. Can be exception in freezing money. This situation can be present when there is some problem in ETH blockchain or with our nodes. ETH/PAX should be frozen later. Or the problem can be in not enough ETH/PAX");
        verifyTransactionInBlock(buyOrderEth.getOrder().getId());

        log.info("Creating BUY Dex Order (PAX)");
        CreateDexOrderResponse buyOrderPax = createDexOrder("15000", "1000", vault1, true, false);
        assertNotNull(buyOrderPax,  "RESPONSE is not correct/dex offer wasn't created");
        assertNotNull(buyOrderPax.getFrozenTx(), "FrozenTx isn't present. Can be exception in freezing money. This situation can be present when there is some problem in ETH blockchain or with our nodes. ETH/PAX should be frozen later. Or the problem can be in not enough ETH/PAX");
        verifyTransactionInBlock(buyOrderPax.getOrder().getId());

        List<DexOrderDto> orders = getDexOrders("0", vault1.getAccountId());
        //TODO: add additional asserts for checking statuses after order was cancelled
        for (DexOrderDto order : orders) {
            if (order.status == 0) {
                verifyTransactionInBlock(dexCancelOrder(order.id, vault1).getTransaction());
                assertEquals(3, getDexOrder(order.id).status, "Status is not equal 3 (cancel)");
            }
            else log.info("orders with status OPEN are not present");
        }


        List<DexOrderDto> ordersDex = getDexOrders();
        assertNotNull(ordersDex);
        assertNotNull(ordersDex.get(0).id);
        assertNotNull(ordersDex.get(0).status);
        assertNotNull(ordersDex.get(0).hasFrozenMoney);
        assertNotNull(ordersDex.get(0).accountId);
        assertNotNull(ordersDex.get(0).fromAddress);
        assertNotNull(ordersDex.get(0).height);
        assertNotNull(ordersDex.get(0).offerAmount);
        assertNotNull(ordersDex.get(0).offerCurrency);
        assertNotNull(ordersDex.get(0).pairCurrency);
        assertNotNull(ordersDex.get(0).pairRate);
        assertNotNull(ordersDex.get(0).type);
    }

    @DisplayName("withdraw ETH/PAX + validation of ETH/PAX balances")
    @Test
    @Execution(SAME_THREAD)
    public void dexWithdrawTransactions() {
        Account2FAResponse balance = getDexBalances(vault2.getEthAddress());
        double ethBalance = balance.getEth().get(0).getBalances().getEth();
        double paxBalance = balance.getEth().get(0).getBalances().getPax();
        assertTrue(ethBalance > 1, "ETH balance is less than 1 ETH");
        assertTrue(paxBalance > 10, "PAX balance is less than 10 PAX");


        EthGasInfoResponse gasPrice = getEthGasInfo();
        assertTrue(Float.valueOf(gasPrice.getFast()) >= Float.valueOf(gasPrice.getAverage()));
        assertTrue(Float.valueOf(gasPrice.getAverage()) >= Float.valueOf(gasPrice.getSafeLow()));
        assertTrue(Float.valueOf(gasPrice.getSafeLow()) > 0);
        Integer fastGas = Math.round(Float.valueOf(gasPrice.getFast()));
        Integer averageGas = Math.round(Float.valueOf(gasPrice.getAverage()));
        Integer safeLowGas = Math.round(Float.valueOf(gasPrice.getSafeLow()));

        //TODO: add assertion and getEthGasFee to include it into tests and validation on balances
        WithdrawResponse withdrawEth = dexWidthraw(vault2.getEthAddress(),
            vault2,
            vault2.getEthAddress(),
                "0.5",
                String.valueOf(averageGas),
                true);
        assertNotNull(withdrawEth.transactionAddress);

        //TODO: add transaction validation is accepted in ETH blockchain
        double newEthBalance = ethBalance - (21000 * 0.000000001 * averageGas);


        WithdrawResponse withdrawPax = dexWidthraw(vault2.getEthAddress(),
            vault2,
            vault2.getEthAddress(),
                "100",
                String.valueOf(fastGas),
                false);
        assertNotNull(withdrawPax.transactionAddress);
        Account2FAResponse balanceValidationPax = getDexBalances(vault2.getEthAddress());

        //PAX balances are the same. All transaction fee is in ETH
        assertEquals(paxBalance, balanceValidationPax.getEth().get(0).getBalances().getPax(), "balances are different");
        double newEthBalanceAfterPax = newEthBalance - (300000 * 0.000000001 * fastGas);

    }

    @DisplayName("dex exchange ETH SELL-BUY")
    @Test
    public void dexExchange() throws InterruptedException {
        String pairRate = "1000";
        String offerAmount = "5000";
        //TODO: balance

        CreateDexOrderResponse sellOrderVault1 = createDexOrder(pairRate, offerAmount, vault1, false, true);
        assertNotNull(sellOrderVault1, "RESPONSE is not correct/dex offer wasn't created");
        verifyTransactionInBlock(sellOrderVault1.getOrder().getId());
        //TODO: change wait on waitHeight method
        Thread.sleep(200000);
        CreateDexOrderResponse buyOrderVault2 = createDexOrder(pairRate, offerAmount, vault2, true, true);
        assertNotNull(buyOrderVault2, "RESPONSE is not correct/dex offer wasn't created");
        assertNotNull(buyOrderVault2.getFrozenTx(), "ETH isn't frozen");
        assertNotNull(buyOrderVault2.getContract(), "CONTRACT isn't created");
        assertNotNull(buyOrderVault2.getOrder(), "ORDER isn't in response");
        verifyTransactionInBlock(buyOrderVault2.getOrder().getId());
        //TODO: change wait on validation by status and transactions
        Thread.sleep(900000);
        assertEquals("5", getDexOrder(sellOrderVault1.getOrder().getId()).status, "SELL order has incorrect status");
        assertEquals("5", getDexOrder(buyOrderVault2.getOrder().getId()).status, "BUY order has incorrect status");
        //TODO: add balance validation APL + ETH
    }


}
