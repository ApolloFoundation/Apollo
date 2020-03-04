package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.BalanceDTO;
import com.apollocurrency.aplwallet.api.dto.DexOrderDto;
import com.apollocurrency.aplwallet.api.dto.TradingDataOutputDTO;
import com.apollocurrency.aplwallet.api.response.Account2FAResponse;
import com.apollocurrency.aplwallet.api.response.CreateDexOrderResponse;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrency.aplwallet.api.response.EthGasInfoResponse;
import com.apollocurrency.aplwallet.api.response.WithdrawResponse;
import com.apollocurrrency.aplwallet.inttest.helper.DexPreconditionExtension;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.model.Parameters;
import com.apollocurrrency.aplwallet.inttest.model.TestBase;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseNew;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseOld;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extensions;
import org.junit.jupiter.api.parallel.Execution;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.parallel.ExecutionMode;


import java.lang.annotation.ElementType;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    private static RetryPolicy retryPolicyDex = new RetryPolicy()
        .retryWhen(false)
        .withMaxRetries(60)
        .withDelay(60, TimeUnit.SECONDS);

    @Step
    private boolean waitForOrderStatus(int status, String orderId){
        log.info("Wait For Status: {}", status);
        boolean isStatus = false;
        try {
            isStatus = Failsafe.with(retryPolicyDex).get(() -> getDexOrder(orderId).status == status);
        } catch (Exception e) {
            fail(String.format("Status %s  not reached. Exception msg: %s", status, e.getMessage()));
        }
        assertTrue(isStatus, String.format("Status %s not reached: %s", status, getDexOrder(orderId).status));
        return isStatus;
    }

    @Step
    private boolean waitForFrozenMoneyStatus(boolean hasFrozenMoney, String orderId){
        log.info("Wait For FrozenMoneyStatus: {}", hasFrozenMoney);
        boolean isStatus = false;
        try {
            isStatus = Failsafe.with(retryPolicyDex).get(() -> getDexOrder(orderId).hasFrozenMoney == hasFrozenMoney);
        } catch (Exception e) {
            fail(String.format("hasFrozenMoney %s  not reached. Exception msg: %s", hasFrozenMoney, e.getMessage()));
        }
        assertTrue(isStatus, String.format("hasFrozenMoney %s not reached: %s", hasFrozenMoney, getDexOrder(orderId).hasFrozenMoney));
        return isStatus;
    }

    @Step
    public void validateDexOrderResponse(String orderId, Integer status, Long pairRate, Wallet wallet, boolean isEth, boolean isSell){
        final int ETH = 1;
        final int PAX = 2;
        final int SELL = 1;
        final int BUY = 0;
        DexOrderDto order = getDexOrder(orderId);
        assertNotNull(order.finishTime, "NO DATA in finishTime field");
        assertNotNull(order.height, "NO DATA in height field");
        assertNotNull(order.offerCurrency, "NO DATA in offerCurrency");
        assertNotNull(order.offerAmount, "NO DATA in offerAmount field");
        assertEquals(status, order.status, "status is correct");
        assertEquals(Long.valueOf(pairRate), order.pairRate, "pair rate is incorrect");
        assertEquals(orderId, order.id, "order Id is correct");
        assertEquals(wallet.getAccountId(), order.accountId, "accountId is incorrect");


        Integer type = (isSell)? SELL : BUY;
        assertEquals(type, order.type, "type is incorrect");

        Integer pairCurrency = (isEth)? ETH : PAX;
        assertEquals(pairCurrency, order.pairCurrency, "pairCurrency is incorrect");

        if (isSell) {
            assertEquals(wallet.getUser(), order.fromAddress, "fromAddress data is incorrect");
            assertEquals(wallet.getEthAddress(), order.toAddress, "toAddress data is incorrect");
        }
        else {
            assertEquals(wallet.getEthAddress(), order.fromAddress, "fromAddress data is incorrect");
            assertEquals(wallet.getUser(), order.toAddress, "toAddress data is incorrect");
        }
    }

    @BeforeAll
    public static void initAll() {
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
    @Execution(SAME_THREAD)
    public void dexExchangeEthSellBuy() {
        //creating parameters
        String pairRate = "1000";
        String offerAmount = "2000";
        Long vault1Fee = 900000000L; // by this dex flow it is apl fee which vault1 should pay for all dex operations
        Long vault2Fee = 800000000L; // by this dex flow it is apl fee which vault2 should pay for all dex operations
        Long aplAmount = Long.valueOf(offerAmount)*100000000;
        Long balanceAplVault1 = getBalance(vault1).getBalanceATM();
        Long balanceAplVault2 = getBalance(vault2).getBalanceATM();
        double balanceEthVault1 = getDexBalances(vault1.getEthAddress()).getEth().get(0).getBalances().getEth();
        double balanceEthVault2 = getDexBalances(vault2.getEthAddress()).getEth().get(0).getBalances().getEth();
        double ethAmount = ((Double.valueOf(pairRate) * Double.valueOf(offerAmount)) * 0.000000001);

        //creating sell dex order transaction and validate response
        CreateDexOrderResponse sellOrderVault1 = createDexOrder(pairRate, offerAmount, vault1, false, true);
        verifyTransactionInBlock(sellOrderVault1.getOrder().getId());
        assertNotNull(sellOrderVault1, "RESPONSE is not correct/dex offer wasn't created");
        assertEquals(0, getDexOrder(sellOrderVault1.getOrder().getId()).status, "STATUS is NOT OPENED");

        //wait 25 blocks and create buy dex order and validate response
        Integer currentHeight = getBlock().getHeight();
        waitForHeight(currentHeight+25);
        CreateDexOrderResponse buyOrderVault2 = createDexOrder(pairRate, offerAmount, vault2, true, true);
        assertNotNull(buyOrderVault2, "RESPONSE is not correct/dex offer wasn't created");
        assertNotNull(buyOrderVault2.getFrozenTx(), "ETH isn't frozen");
        assertNotNull(buyOrderVault2.getContract(), "CONTRACT isn't created");
        assertNotNull(buyOrderVault2.getOrder(), "ORDER isn't in response");
        verifyTransactionInBlock(buyOrderVault2.getOrder().getId());

        //waiting statuses are correct and dex exchange is finished (order status is closed and frozenMoney = false)
        waitForOrderStatus(5, sellOrderVault1.getOrder().getId());
        waitForOrderStatus(5, buyOrderVault2.getOrder().getId());
        waitForFrozenMoneyStatus(false, buyOrderVault2.getOrder().getId());

        //validate APL balance + ETH balance
        assertEquals(balanceAplVault1-vault1Fee-aplAmount, getBalance(vault1).getBalanceATM(), "APL BALANCE validation isn't passed on VAULT1");
        assertEquals(balanceAplVault2-vault2Fee+aplAmount, getBalance(vault2).getBalanceATM(), "APL BALANCE validation isn't passed on VAULT2");
        assertTrue(getDexBalances(vault1.getEthAddress()).getEth().get(0).getBalances().getEth() > balanceEthVault1, "ETH balance of vault1 (SELL ACCOUNT) didn't become more than it was");
        assertTrue(getDexBalances(vault2.getEthAddress()).getEth().get(0).getBalances().getEth() < (balanceEthVault2 - ethAmount), "ETH balance of vault2 (BUY ACCOUNT) didn't become less than it was ");
        assertTrue(getDexBalances(vault1.getEthAddress()).getEth().get(0).getBalances().getEth() < (balanceEthVault1 + ethAmount), "ETH balance of vault1 (SELL ACCOUNT) is more than should be! ");

        //TODO: edit ETH balance validation (should include eth comission)
        //TODO: add transaction validation on each account
        //TODO: add order status validation on each account
        //TODO: add trade history (closed) validation

        //validate dex history and closed orders for account
        /*TradingDataOutputDTO dexTrades = getDexTradeInfo(true, "15");
        BigDecimal offerAmountBigDecimal = new BigDecimal(offerAmount);


        softAssertions.assertThat(dexTrades.getV().get(0).compareTo(offerAmountBigDecimal)).isGreaterThanOrEqualTo(0);
        softAssertions.assertThat(getDexHistory(vault1.getAccountId(), true, true).stream().findFirst().get().id).isEqualTo(sellOrderVault1.getOrder().getId());
        softAssertions.assertThat(getDexHistory(vault2.getAccountId(), true, false).stream().findFirst().get().id).isEqualTo(buyOrderVault2.getOrder().getId());
        softAssertions.assertAll();
        assertAll(
            () -> assertThat(dexTrades.getV().get(0).compareTo(offerAmountBigDecimal), greaterThan(10)),
            () -> assertThat(getDexHistory(vault1.getAccountId(), true, true).stream().findFirst().get().id, equals(sellOrderVault1.getOrder().getId())),
            () -> assertThat(getDexHistory(vault2.getAccountId(), true, false).stream().findFirst().get().id, equals(buyOrderVault2.getOrder().getId()))
        );    */

        validateDexOrderResponse(sellOrderVault1.getOrder().getId(), 5, Long.valueOf(pairRate), vault1, true, true);
        validateDexOrderResponse(buyOrderVault2.getOrder().getId(), 5, Long.valueOf(pairRate), vault2, true, false);
    }

    @DisplayName("dex exchange ETH BUY-SELL")
    @Test
    @Execution(SAME_THREAD)
    public void dexExchangeEthBuySell() {
        //creating parameters
        String pairRate = "1000";
        String offerAmount = "3000";
        Long vault1Fee = 600000000L; // by this dex flow it is apl fee which vault1 should pay for all dex operations
        Long vault2Fee = 1100000000L; // by this dex flow it is apl fee which vault2 should pay for all dex operations
        Long aplAmount = Long.valueOf(offerAmount)*100000000;
        Long balanceAplVault1 = getBalance(vault1).getBalanceATM();
        Long balanceAplVault2 = getBalance(vault2).getBalanceATM();
        double balanceEthVault1 = getDexBalances(vault1.getEthAddress()).getEth().get(0).getBalances().getEth();
        double balanceEthVault2 = getDexBalances(vault2.getEthAddress()).getEth().get(0).getBalances().getEth();
        double ethAmount = ((Double.valueOf(pairRate) * Double.valueOf(offerAmount)) * 0.000000001);

        //creating BUY dex order transaction and validate response
        CreateDexOrderResponse buyOrderVault1 = createDexOrder(pairRate, offerAmount, vault1, true, true);
        verifyTransactionInBlock(buyOrderVault1.getOrder().getId());
        assertNotNull(buyOrderVault1, "RESPONSE is not correct/dex offer wasn't created");
        assertNotNull(buyOrderVault1.getFrozenTx(), "ETH is n't frozen");
        assertEquals(0, getDexOrder(buyOrderVault1.getOrder().getId()).status, "STATUS is NOT OPENED");

        //wait 25 blocks and create SELL dex order and validate response
        Integer currentHeight = getBlock().getHeight();
        waitForHeight(currentHeight+25);
        CreateDexOrderResponse sellOrderVault2 = createDexOrder(pairRate, offerAmount, vault2, false, true);
        assertNotNull(sellOrderVault2, "RESPONSE is not correct/dex offer wasn't created");
        assertNotNull(sellOrderVault2.getContract(), "CONTRACT isn't created");
        assertNotNull(sellOrderVault2.getOrder(), "ORDER isn't in response");
        verifyTransactionInBlock(sellOrderVault2.getOrder().getId());

        //waiting statuses are correct and dex exchange is finished (order status is closed and frozenMoney = false)
        waitForOrderStatus(5, sellOrderVault2.getOrder().getId());
        waitForOrderStatus(5, buyOrderVault1.getOrder().getId());
        waitForFrozenMoneyStatus(false, buyOrderVault1.getOrder().getId());

        //validate APL balance + ETH balance
        assertEquals(balanceAplVault1-vault1Fee+aplAmount, getBalance(vault1).getBalanceATM(), "APL BALANCE validation isn't passed on VAULT1");
        assertEquals(balanceAplVault2-vault2Fee-aplAmount, getBalance(vault2).getBalanceATM(), "APL BALANCE validation isn't passed on VAULT2");
        assertTrue(getDexBalances(vault2.getEthAddress()).getEth().get(0).getBalances().getEth() > balanceEthVault2, "ETH balance of vault2 (SELL ACCOUNT) didn't become more than it was");
        assertTrue(getDexBalances(vault1.getEthAddress()).getEth().get(0).getBalances().getEth() < (balanceEthVault1 - ethAmount), "ETH balance of vault1 (BUY ACCOUNT) didn't become less than it was ");
        assertTrue(getDexBalances(vault2.getEthAddress()).getEth().get(0).getBalances().getEth() < (balanceEthVault2 + ethAmount), "ETH balance of vault2 (SELL ACCOUNT) is more than should be! ");

        //TODO: edit ETH balance validation (should include eth comission)
        //TODO: add transaction validation on each account
        //TODO: add order status validation on each account
        //TODO: add trade history (closed) validation

        //validate dex history and closed orders for account
        /*TradingDataOutputDTO dexTrades = getDexTradeInfo(true, "15");
        BigDecimal offerAmountBigDecimal = new BigDecimal(offerAmount);


        softAssertions.assertThat(dexTrades.getV().get(0).compareTo(offerAmountBigDecimal)).isGreaterThanOrEqualTo(0);
        softAssertions.assertThat(getDexHistory(vault1.getAccountId(), true, true).stream().findFirst().get().id).isEqualTo(sellOrderVault1.getOrder().getId());
        softAssertions.assertThat(getDexHistory(vault2.getAccountId(), true, false).stream().findFirst().get().id).isEqualTo(buyOrderVault2.getOrder().getId());
        softAssertions.assertAll();
        assertAll(
            () -> assertThat(dexTrades.getV().get(0).compareTo(offerAmountBigDecimal), greaterThan(10)),
            () -> assertThat(getDexHistory(vault1.getAccountId(), true, true).stream().findFirst().get().id, equals(sellOrderVault1.getOrder().getId())),
            () -> assertThat(getDexHistory(vault2.getAccountId(), true, false).stream().findFirst().get().id, equals(buyOrderVault2.getOrder().getId()))
        );    */

        validateDexOrderResponse(sellOrderVault2.getOrder().getId(), 5, Long.valueOf(pairRate), vault2, true, true);
        validateDexOrderResponse(buyOrderVault1.getOrder().getId(), 5, Long.valueOf(pairRate), vault1, true, false);
    }

    @DisplayName("dex exchange PAX SELL-BUY")
    @Test
    @Execution(SAME_THREAD)
    public void dexExchangePaxSellBuy() {
        //creating parameters
        String pairRate = "1000";
        String offerAmount = "5000";
        Long vault1Fee = 900000000L; // by this dex flow it is apl fee which vault1 should pay for all dex operations
        Long vault2Fee = 800000000L; // by this dex flow it is apl fee which vault2 should pay for all dex operations
        Long aplAmount = Long.valueOf(offerAmount)*100000000;
        Long balanceAplVault1 = getBalance(vault1).getBalanceATM();
        Long balanceAplVault2 = getBalance(vault2).getBalanceATM();
        double balancePaxVault1 = getDexBalances(vault1.getEthAddress()).getEth().get(0).getBalances().getPax();
        double balancePaxVault2 = getDexBalances(vault2.getEthAddress()).getEth().get(0).getBalances().getPax();
        double paxAmount = ((Double.valueOf(pairRate) * Double.valueOf(offerAmount)) * 0.000000001);

        //creating sell dex order transaction and validate response
        CreateDexOrderResponse sellOrderVault1 = createDexOrder(pairRate, offerAmount, vault1, false, false);
        verifyTransactionInBlock(sellOrderVault1.getOrder().getId());
        assertNotNull(sellOrderVault1, "RESPONSE is not correct/dex offer wasn't created");
        assertEquals(0, getDexOrder(sellOrderVault1.getOrder().getId()).status, "STATUS is NOT OPENED");

        //wait 25 blocks and create buy dex order and validate response
        Integer currentHeight = getBlock().getHeight();
        waitForHeight(currentHeight+25);
        CreateDexOrderResponse buyOrderVault2 = createDexOrder(pairRate, offerAmount, vault2, true, false);
        assertNotNull(buyOrderVault2, "RESPONSE is not correct/dex offer wasn't created");
        assertNotNull(buyOrderVault2.getFrozenTx(), "PAX isn't frozen");
        assertNotNull(buyOrderVault2.getContract(), "CONTRACT isn't created");
        assertNotNull(buyOrderVault2.getOrder(), "ORDER isn't in response");
        verifyTransactionInBlock(buyOrderVault2.getOrder().getId());

        //waiting statuses are correct and dex exchange is finished (order status is closed and frozenMoney = false)
        waitForOrderStatus(5, sellOrderVault1.getOrder().getId());
        waitForOrderStatus(5, buyOrderVault2.getOrder().getId());
        waitForFrozenMoneyStatus(false, buyOrderVault2.getOrder().getId());

        //validate APL balance + ETH balance
        assertEquals(balanceAplVault1-vault1Fee-aplAmount, getBalance(vault1).getBalanceATM(), "APL BALANCE validation isn't passed on VAULT1");
        assertEquals(balanceAplVault2-vault2Fee+aplAmount, getBalance(vault2).getBalanceATM(), "APL BALANCE validation isn't passed on VAULT2");

        assertEquals(balancePaxVault1 + paxAmount, getDexBalances(vault1.getEthAddress()).getEth().get(0).getBalances().getPax(), "PAX balance of vault1 (SELL ACCOUNT) isn't correct");
        assertEquals(balancePaxVault2 - paxAmount, getDexBalances(vault2.getEthAddress()).getEth().get(0).getBalances().getPax(), "PAX balance of vault2 (BUY ACCOUNT) isn't correct ");

        //TODO: edit ETH balance validation (should include eth comission)
        //TODO: add transaction validation on each account
        //TODO: add order status validation on each account
        //TODO: add trade history (closed) validation

        //validate dex history and closed orders for account
        /*TradingDataOutputDTO dexTrades = getDexTradeInfo(true, "15");
        BigDecimal offerAmountBigDecimal = new BigDecimal(offerAmount);


        softAssertions.assertThat(dexTrades.getV().get(0).compareTo(offerAmountBigDecimal)).isGreaterThanOrEqualTo(0);
        softAssertions.assertThat(getDexHistory(vault1.getAccountId(), true, true).stream().findFirst().get().id).isEqualTo(sellOrderVault1.getOrder().getId());
        softAssertions.assertThat(getDexHistory(vault2.getAccountId(), true, false).stream().findFirst().get().id).isEqualTo(buyOrderVault2.getOrder().getId());
        softAssertions.assertAll();
        assertAll(
            () -> assertThat(dexTrades.getV().get(0).compareTo(offerAmountBigDecimal), greaterThan(10)),
            () -> assertThat(getDexHistory(vault1.getAccountId(), true, true).stream().findFirst().get().id, equals(sellOrderVault1.getOrder().getId())),
            () -> assertThat(getDexHistory(vault2.getAccountId(), true, false).stream().findFirst().get().id, equals(buyOrderVault2.getOrder().getId()))
        );    */

        validateDexOrderResponse(sellOrderVault1.getOrder().getId(), 5, Long.valueOf(pairRate), vault1, false, true);
        validateDexOrderResponse(buyOrderVault2.getOrder().getId(), 5, Long.valueOf(pairRate), vault2, false, false);
    }

    @DisplayName("dex exchange PAX BUY-SELL")
    @Test
    @Execution(SAME_THREAD)
    public void dexExchangePaxBuySell() {
        //creating parameters
        String pairRate = "1000";
        String offerAmount = "4000";
        Long vault1Fee = 600000000L; // by this dex flow it is apl fee which vault1 should pay for all dex operations
        Long vault2Fee = 1100000000L; // by this dex flow it is apl fee which vault2 should pay for all dex operations
        Long aplAmount = Long.valueOf(offerAmount)*100000000;
        Long balanceAplVault1 = getBalance(vault1).getBalanceATM();
        Long balanceAplVault2 = getBalance(vault2).getBalanceATM();
        double balancePaxVault1 = getDexBalances(vault1.getEthAddress()).getEth().get(0).getBalances().getPax();
        double balancePaxVault2 = getDexBalances(vault2.getEthAddress()).getEth().get(0).getBalances().getPax();
        double paxAmount = ((Double.valueOf(pairRate) * Double.valueOf(offerAmount)) * 0.000000001);

        //creating BUY dex order transaction and validate response
        CreateDexOrderResponse buyOrderVault1 = createDexOrder(pairRate, offerAmount, vault1, true, false);
        verifyTransactionInBlock(buyOrderVault1.getOrder().getId());
        assertNotNull(buyOrderVault1, "RESPONSE is not correct/dex offer wasn't created");
        assertNotNull(buyOrderVault1.getFrozenTx(), "ETH/PAX is n't frozen");
        assertEquals(0, getDexOrder(buyOrderVault1.getOrder().getId()).status, "STATUS is NOT OPENED");

        //wait 25 blocks and create SELL dex order and validate response
        Integer currentHeight = getBlock().getHeight();
        waitForHeight(currentHeight+25);
        CreateDexOrderResponse sellOrderVault2 = createDexOrder(pairRate, offerAmount, vault2, false, false);
        assertNotNull(sellOrderVault2, "RESPONSE is not correct/dex offer wasn't created");
        assertNotNull(sellOrderVault2.getContract(), "CONTRACT isn't created");
        assertNotNull(sellOrderVault2.getOrder(), "ORDER isn't in response");
        verifyTransactionInBlock(sellOrderVault2.getOrder().getId());

        //waiting statuses are correct and dex exchange is finished (order status is closed and frozenMoney = false)
        waitForOrderStatus(5, sellOrderVault2.getOrder().getId());
        waitForOrderStatus(5, buyOrderVault1.getOrder().getId());
        waitForFrozenMoneyStatus(false, buyOrderVault1.getOrder().getId());

        //validate APL balance + ETH balance
        assertEquals(balanceAplVault1-vault1Fee+aplAmount, getBalance(vault1).getBalanceATM(), "APL BALANCE validation isn't passed on VAULT1");
        assertEquals(balanceAplVault2-vault2Fee-aplAmount, getBalance(vault2).getBalanceATM(), "APL BALANCE validation isn't passed on VAULT2");
        assertEquals(balancePaxVault2 + paxAmount, getDexBalances(vault2.getEthAddress()).getEth().get(0).getBalances().getPax(), "PAX balance of vault2 (SELL ACCOUNT) isn't correct");
        assertEquals(balancePaxVault1 - paxAmount, getDexBalances(vault1.getEthAddress()).getEth().get(0).getBalances().getPax(), "PAX balance of vault1 (BUY ACCOUNT) isn't correct ");


        //TODO: edit ETH balance validation (should include eth comission)
        //TODO: add transaction validation on each account
        //TODO: add order status validation on each account
        //TODO: add trade history (closed) validation

        //validate dex history and closed orders for account
        /*TradingDataOutputDTO dexTrades = getDexTradeInfo(true, "15");
        BigDecimal offerAmountBigDecimal = new BigDecimal(offerAmount);


        softAssertions.assertThat(dexTrades.getV().get(0).compareTo(offerAmountBigDecimal)).isGreaterThanOrEqualTo(0);
        softAssertions.assertThat(getDexHistory(vault1.getAccountId(), true, true).stream().findFirst().get().id).isEqualTo(sellOrderVault1.getOrder().getId());
        softAssertions.assertThat(getDexHistory(vault2.getAccountId(), true, false).stream().findFirst().get().id).isEqualTo(buyOrderVault2.getOrder().getId());
        softAssertions.assertAll();
        assertAll(
            () -> assertThat(dexTrades.getV().get(0).compareTo(offerAmountBigDecimal), greaterThan(10)),
            () -> assertThat(getDexHistory(vault1.getAccountId(), true, true).stream().findFirst().get().id, equals(sellOrderVault1.getOrder().getId())),
            () -> assertThat(getDexHistory(vault2.getAccountId(), true, false).stream().findFirst().get().id, equals(buyOrderVault2.getOrder().getId()))
        );    */

        validateDexOrderResponse(sellOrderVault2.getOrder().getId(), 5, Long.valueOf(pairRate), vault2, false, true);
        validateDexOrderResponse(buyOrderVault1.getOrder().getId(), 5, Long.valueOf(pairRate), vault1, false, false);
    }
}

