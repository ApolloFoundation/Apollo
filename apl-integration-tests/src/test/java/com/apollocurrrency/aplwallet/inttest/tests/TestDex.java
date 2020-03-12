package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.DexOrderDto;
import com.apollocurrency.aplwallet.api.dto.TradingDataOutputDTO;
import com.apollocurrency.aplwallet.api.response.Account2FAResponse;
import com.apollocurrency.aplwallet.api.response.CreateDexOrderResponse;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrency.aplwallet.api.response.DexAccountInfoResponse;
import com.apollocurrency.aplwallet.api.response.EthGasInfoResponse;
import com.apollocurrency.aplwallet.api.response.WithdrawResponse;
import com.apollocurrrency.aplwallet.inttest.helper.DexPreconditionExtension;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.model.TestBase;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseNew;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extensions;
import org.junit.jupiter.api.parallel.Execution;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
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
    private boolean waitEmptyFilledOrders(String ethWallet){
        log.info("Wait For ETh wallet empty: {}", ethWallet);
        boolean isStatus = false;
        try {
            isStatus = Failsafe.with(retryPolicyDex).get(() -> getFilledOrders().stream().filter(ordersInfo -> ordersInfo.getAddress().equals(ethWallet)).findFirst().get().getDepositsInfo().isEmpty());
        } catch (Exception e) {
            fail(String.format("DepositInfo %s  not reached. Exception msg: %s", ethWallet, e.getMessage()));
        }
        assertTrue(isStatus, "deposit Info is still NOT EMPTY");
        return isStatus;
    }

    @Step
    private boolean waitEmptyActiveOrders(String ethWallet){
        log.info("Wait For ETh wallet empty: {}", ethWallet);
        boolean isStatus = false;
        try {
            isStatus = Failsafe.with(retryPolicyDex).get(() -> getActiveDeposits().stream().filter(ordersInfo -> ordersInfo.getAddress().equals(ethWallet)).findFirst().get().getDepositsInfo().isEmpty());
        } catch (Exception e) {
            fail(String.format("DepositInfo %s  not reached. Exception msg: %s", ethWallet, e.getMessage()));
        }
        assertTrue(isStatus, "deposit Info is still NOT EMPTY");
        return isStatus;
    }

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
    public void assertNotNullGetDexOrders(){
        List<DexOrderDto> ordersDex = getDexOrders();
        assertNotNull(ordersDex);
        assertNotNull(ordersDex.get(0));
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

    @Step
    public void validateDexOrderResponse(String orderId, Integer status, Long pairRate, Wallet wallet, boolean isEth, boolean isSell){
        final int ETH = 1;
        final int PAX = 2;
        final int SELL = 1;
        final int BUY = 0;
        Integer type = (isSell)? SELL : BUY;
        Integer pairCurrency = (isEth)? ETH : PAX;
        DexOrderDto order = getDexOrder(orderId);
        assertAll(
            () -> assertNotNull(order.finishTime, "NO DATA in finishTime field"),
            () -> assertNotNull(order.height, "NO DATA in height field"),
            () -> assertNotNull(order.offerCurrency, "NO DATA in offerCurrency"),
            () -> assertNotNull(order.offerAmount, "NO DATA in offerAmount field"),
            () -> assertEquals(status, order.status, "status is correct"),
            () -> assertEquals(Long.valueOf(pairRate), order.pairRate, "pair rate is incorrect"),
            () -> assertEquals(orderId, order.id, "order Id is correct"),
            () -> assertEquals(wallet.getAccountId(), order.accountId, "accountId is incorrect"),
            () -> assertEquals(type, order.type, "type is incorrect"),
            () -> assertEquals(pairCurrency, order.pairCurrency, "pairCurrency is incorrect"),
            () -> {if (isSell) {
                assertEquals(wallet.getUser(), order.fromAddress, "fromAddress data is incorrect");
                assertEquals(wallet.getEthAddress(), order.toAddress, "toAddress data is incorrect");
            }
            else {
                assertEquals(wallet.getEthAddress(), order.fromAddress, "fromAddress data is incorrect");
                assertEquals(wallet.getUser(), order.toAddress, "toAddress data is incorrect");
            }}
        );

    }

    @BeforeEach
    public void logInDexExchange(){
        DexAccountInfoResponse vault1Log = logInDex(vault1);
        assertTrue(vault1Log.getCurrencies().get(0).getCurrency().equals("eth"), "response isn't correct for vault1");
        assertTrue(vault1Log.getCurrencies().get(0).getWallets().get(0).getAddress().equals(vault1.getEthAddress()), "response isn't correct for vault1");
        DexAccountInfoResponse vault2Log = logInDex(vault2);
        assertTrue(vault2Log.getCurrencies().get(0).getCurrency().equals("eth"), "response isn't correct for vault2");
        assertTrue(vault2Log.getCurrencies().get(0).getWallets().get(0).getAddress().equals(vault2.getEthAddress()), "response isn't correct for vault2");
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
        }
        if (getBalanceSetUP(vault2).getBalanceATM() < 90000000000000L) {
            log.info("Send money on: " + vault2);

            transactionResponse = sendMoneySetUp(TestConfiguration.getTestConfiguration().getGenesisWallet(),
                vault2.getUser(), 1000000);
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

    //TODO: divide cancel on four different tests
    @DisplayName("CANCEL SELL APL/ETH DEX ORDER")
    @Test
    @Execution(SAME_THREAD)
    public void cancelSellEth(){
        log.info("Creating SELL Dex Order (ETH)");
        final Long APL_FEE = 300000000L;
        final Long BALANCE_APL = getBalance(vault1).getBalanceATM();
        CreateDexOrderResponse sellOrderEth = createDexOrder("30000", "1000", vault1, false, true);
        String orderId = sellOrderEth.getOrder().getId();
        assertNotNull(sellOrderEth, "RESPONSE is not correct/dex offer wasn't created");
        assertNotNull(orderId);
        verifyTransactionInBlock(orderId);
        assertEquals(0, getDexOrder(orderId).status, "status isn't opened/canceling functionality will not work/reason: is in dex proccess.");
        CreateTransactionResponse cancelOrder = dexCancelOrder(orderId, vault1);
        verifyTransactionInBlock(cancelOrder.getTransaction());
        assertEquals(3, getDexOrder(orderId).status, "status isn't cancelled (3)");
        assertEquals(BALANCE_APL-APL_FEE, getBalance(vault1).getBalanceATM(), "APL BALANCE ISN'T CORRECT.");
        assertNotNullGetDexOrders();
    }

    @DisplayName("CANCEL SELL APL/PAX DEX ORDER")
    @Test
    @Execution(SAME_THREAD)
    public void cancelSellPax(){

        final Long APL_FEE = 300000000L;
        final Long BALANCE_APL = getBalance(vault1).getBalanceATM();

        log.info("Create SELL Dex Order (PAX)");
        CreateDexOrderResponse sellOrderPax = createDexOrder("40000", "1000", vault1, false, false);
        assertNotNull(sellOrderPax, "RESPONSE is not correct/dex offer wasn't created");
        assertNotNull(sellOrderPax.getOrder().getId());
        verifyTransactionInBlock(sellOrderPax.getOrder().getId());
        String orderId = sellOrderPax.getOrder().getId();
        assertEquals(0, getDexOrder(orderId).status, "status isn't opened. Canceling functionality will not work. Reason: is in dex proccess.");
        //cancel sellOrder
        CreateTransactionResponse cancelOrder = dexCancelOrder(orderId, vault1);
        assertNotNull(cancelOrder);
        verifyTransactionInBlock(cancelOrder.getTransaction());
        assertEquals(3, getDexOrder(orderId).status, "status isn't cancelled (3)");
        assertEquals(BALANCE_APL-APL_FEE, getBalance(vault1).getBalanceATM(), "APL BALANCE ISN'T CORRECT.");
        assertNotNullGetDexOrders();
    }

    @DisplayName("CANCEL BUY ETH ORDER")
    @Test
    @Execution(SAME_THREAD)
    public void cancelBuyEth(){
        final String PAIR_RATE = "15000";
        final String OFFER_AMOUNT = "1000";
        final Long APL_FEE = 300000000L;
        final Long BALANCE_APL = getBalance(vault1).getBalanceATM();
        double balanceEthVault1 = getDexBalances(vault1.getEthAddress()).getEth().get(0).getBalances().getEth();

        log.info("Creating BUY Dex Order (ETH)");
        CreateDexOrderResponse buyOrderEth = createDexOrder(PAIR_RATE, OFFER_AMOUNT, vault1, true, true);
        assertNotNull(buyOrderEth,  "RESPONSE is not correct/dex offer wasn't created");
        assertNotEquals("Exception in the process of freezing money.", buyOrderEth.errorDescription, "Exception in the process of freezing money. Problem with ETH node.");
        verifyTransactionInBlock(buyOrderEth.getOrder().getId());
        String orderId = buyOrderEth.getOrder().getId();
        assertNotNull(buyOrderEth.getFrozenTx(), "FrozenTx isn't present. Can be exception in freezing money. This situation can be present when there is some problem in ETH blockchain or with our nodes. ETH/PAX should be frozen later. Or the problem can be in not enough ETH/PAX");
        assertEquals(0, getDexOrder(orderId).status, "status isn't opened. Canceling functionality will not work. Reason: is in dex proccess.");
        waitForFrozenMoneyStatus(true, orderId);
        //cancel sellOrder
        CreateTransactionResponse cancelOrder = dexCancelOrder(orderId, vault1);
        assertNotNull(cancelOrder);
        verifyTransactionInBlock(cancelOrder.getTransaction());
        assertEquals(3, getDexOrder(orderId).status, "status isn't cancelled (3)");
        assertEquals(BALANCE_APL-APL_FEE, getBalance(vault1).getBalanceATM(), "APL BALANCE ISN'T CORRECT.");
        waitForFrozenMoneyStatus(false, orderId);
        waitEmptyActiveOrders(vault1.getEthAddress());
        assertThat("ETH balance is too different", getDexBalances(vault1.getEthAddress()).getEth().get(0).getBalances().getEth(), closeTo((balanceEthVault1), 0.003));
        assertNotNullGetDexOrders();
    }

    @DisplayName("CANCEL BUY PAX ORDER")
    @Test
    @Execution(SAME_THREAD)
    public void cancelBuyPax(){
        final String PAIR_RATE = "20000";
        final String OFFER_AMOUNT = "1000";
        final Long APL_FEE = 300000000L;
        final Long BALANCE_APL = getBalance(vault1).getBalanceATM();
        double balancePAX = getDexBalances(vault1.getEthAddress()).getEth().get(0).getBalances().getPax();

        log.info("Creating BUY Dex Order (PAX)");
        CreateDexOrderResponse buyOrderEth = createDexOrder(PAIR_RATE, OFFER_AMOUNT, vault1, true, false);
        assertNotNull(buyOrderEth,  "RESPONSE is not correct/dex offer wasn't created");
        assertNotEquals("Exception in the process of freezing money.", buyOrderEth.errorDescription, "Exception in the process of freezing money. Problem with ETH node.");
        verifyTransactionInBlock(buyOrderEth.getOrder().getId());
        String orderId = buyOrderEth.getOrder().getId();
        assertNotNull(buyOrderEth.getFrozenTx(), "FrozenTx isn't present. Can be exception in freezing money. This situation can be present when there is some problem in ETH blockchain or with our nodes. ETH/PAX should be frozen later. Or the problem can be in not enough ETH/PAX");
        assertEquals(0, getDexOrder(orderId).status, "status isn't opened. Canceling functionality will not work. Reason: is in dex proccess.");
        waitForFrozenMoneyStatus(true, orderId);
        //cancel sellOrder
        CreateTransactionResponse cancelOrder = dexCancelOrder(orderId, vault1);
        assertNotNull(cancelOrder);
        verifyTransactionInBlock(cancelOrder.getTransaction());
        waitEmptyActiveOrders(vault1.getEthAddress());
        waitForFrozenMoneyStatus(false, orderId);
        assertEquals(3, getDexOrder(orderId).status, "status isn't cancelled (3)");
        assertEquals(BALANCE_APL-APL_FEE, getBalance(vault1).getBalanceATM(), "APL BALANCE ISN'T CORRECT.");
        assertEquals(balancePAX, getDexBalances(vault1.getEthAddress()).getEth().get(0).getBalances().getPax(), "PAX BALANCE IS WRONG");
        assertNotNullGetDexOrders();
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

    @DisplayName("DEX EXCHANGE ETH SELL-BUY")
    @Test
    @Execution(SAME_THREAD)
    public void dexExchangeEthSellBuy() {
        //initializing parameters
        final String PAIR_RATE = "1000";
        final String OFFER_AMOUNT = "2000";
        final Long VAULT1_FEE = 900000000L; // by this dex flow it is apl fee which vault1 should pay for all dex operations
        final Long VAULT2_FEE = 800000000L; // by this dex flow it is apl fee which vault2 should pay for all dex operations
        final Long APL_AMOUNT = Long.valueOf(OFFER_AMOUNT)*100000000;
        final Long BALANCE_APL_VAULT1 = getBalance(vault1).getBalanceATM();
        final Long BALANCE_APL_VAULT2 = getBalance(vault2).getBalanceATM();
        double balanceEthVault1 = getDexBalances(vault1.getEthAddress()).getEth().get(0).getBalances().getEth();
        double balanceEthVault2 = getDexBalances(vault2.getEthAddress()).getEth().get(0).getBalances().getEth();
        double ethAmount = ((Double.valueOf(PAIR_RATE) * Double.valueOf(OFFER_AMOUNT)) * 0.000000001);

        //creating sell dex order transaction and validate response
        CreateDexOrderResponse sellOrderVault1 = createDexOrder(PAIR_RATE, OFFER_AMOUNT, vault1, false, true);
        verifyTransactionInBlock(sellOrderVault1.getOrder().getId());
        assertNotNull(sellOrderVault1, "RESPONSE is not correct/dex offer wasn't created");
        assertEquals(0, getDexOrder(sellOrderVault1.getOrder().getId()).status, "STATUS is NOT OPENED");

        //wait 25 blocks and create buy dex order and validate response
        Integer currentHeight = getBlock().getHeight();
        waitForHeight(currentHeight+25);
        CreateDexOrderResponse buyOrderVault2 = createDexOrder(PAIR_RATE, OFFER_AMOUNT, vault2, true, true);
        assertNotEquals("Exception in the process of freezing money.", buyOrderVault2.errorDescription, "Exception in the process of freezing money. Problem with ETH node.");
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
        assertEquals(BALANCE_APL_VAULT1-VAULT1_FEE-APL_AMOUNT, getBalance(vault1).getBalanceATM(), "APL BALANCE validation isn't passed on VAULT1");
        assertEquals(BALANCE_APL_VAULT2-VAULT2_FEE+APL_AMOUNT, getBalance(vault2).getBalanceATM(), "APL BALANCE validation isn't passed on VAULT2");

        waitEmptyFilledOrders(vault2.getEthAddress());

        assertThat("ETH balance of vault1 (SELL ACCOUNT) didn't become more than it was", getDexBalances(vault1.getEthAddress()).getEth().get(0).getBalances().getEth(), closeTo((balanceEthVault1 + ethAmount), 0.002));
        assertThat("ETH balance of vault2 (BUY ACCOUNT) didn't become less than it was", getDexBalances(vault2.getEthAddress()).getEth().get(0).getBalances().getEth(), closeTo((balanceEthVault2 - ethAmount), 0.005));

        //TODO: edit ETH balance validation (should include eth comission)
        //TODO: add transaction validation on each account
        //TODO: add order status validation on each account

        //validate dex history and closed orders for account  NOW BUG IS HERE, SHOULD WORK
        /*TradingDataOutputDTO dexTrades = getDexTradeInfo(true, "15");
        BigDecimal offerAmountBigDecimal = new BigDecimal(OFFER_AMOUNT);
        assertTrue(dexTrades.getV().get(0).compareTo(offerAmountBigDecimal) >= 0, "dex trade isn't shown on trading view");*/

        validateDexOrderResponse(sellOrderVault1.getOrder().getId(), 5, Long.valueOf(PAIR_RATE), vault1, true, true);
        validateDexOrderResponse(buyOrderVault2.getOrder().getId(), 5, Long.valueOf(PAIR_RATE), vault2, true, false);
    }

    @DisplayName("DEX EXCHANGE ETH BUY-SELL")
    @Test
    @Execution(SAME_THREAD)
    public void dexExchangeEthBuySell() {
        //creating parameters
        final String PAIR_RATE = "1000";
        final String OFFER_AMOUNT = "3000";
        final Long VAULT1_FEE = 600000000L; // by this dex flow it is apl fee which vault1 should pay for all dex operations
        final Long VAULT2_FEE = 1100000000L; // by this dex flow it is apl fee which vault2 should pay for all dex operations
        final Long APL_AMOUNT = Long.valueOf(OFFER_AMOUNT)*100000000;
        final Long BALANCE_APL_VAULT1 = getBalance(vault1).getBalanceATM();
        final Long BALANCE_APL_VAULT2 = getBalance(vault2).getBalanceATM();
        double balanceEthVault1 = getDexBalances(vault1.getEthAddress()).getEth().get(0).getBalances().getEth();
        double balanceEthVault2 = getDexBalances(vault2.getEthAddress()).getEth().get(0).getBalances().getEth();
        double ethAmount = ((Double.valueOf(PAIR_RATE) * Double.valueOf(OFFER_AMOUNT)) * 0.000000001);

        //creating BUY dex order transaction and validate response
        CreateDexOrderResponse buyOrderVault1 = createDexOrder(PAIR_RATE, OFFER_AMOUNT, vault1, true, true);
        assertNotEquals("Exception in the process of freezing money.", buyOrderVault1.errorDescription, "Exception in the process of freezing money. Problem with ETH node.");
        verifyTransactionInBlock(buyOrderVault1.getOrder().getId());
        assertNotNull(buyOrderVault1, "RESPONSE is not correct/dex offer wasn't created");
        assertNotNull(buyOrderVault1.getFrozenTx(), "ETH is n't frozen");
        assertEquals(0, getDexOrder(buyOrderVault1.getOrder().getId()).status, "STATUS is NOT OPENED");

        //wait 25 blocks and create SELL dex order and validate response
        Integer currentHeight = getBlock().getHeight();
        waitForHeight(currentHeight+25);
        CreateDexOrderResponse sellOrderVault2 = createDexOrder(PAIR_RATE, OFFER_AMOUNT, vault2, false, true);
        assertNotNull(sellOrderVault2, "RESPONSE is not correct/dex offer wasn't created");
        assertNotNull(sellOrderVault2.getContract(), "CONTRACT isn't created");
        assertNotNull(sellOrderVault2.getOrder(), "ORDER isn't in response");
        verifyTransactionInBlock(sellOrderVault2.getOrder().getId());

        //waiting statuses are correct and dex exchange is finished (order status is closed and frozenMoney = false)
        waitForOrderStatus(5, sellOrderVault2.getOrder().getId());
        waitForOrderStatus(5, buyOrderVault1.getOrder().getId());
        waitForFrozenMoneyStatus(false, buyOrderVault1.getOrder().getId());

        //validate APL balance + ETH balance
        assertEquals(BALANCE_APL_VAULT1-VAULT1_FEE+APL_AMOUNT, getBalance(vault1).getBalanceATM(), "APL BALANCE validation isn't passed on VAULT1");
        assertEquals(BALANCE_APL_VAULT2-VAULT2_FEE-APL_AMOUNT, getBalance(vault2).getBalanceATM(), "APL BALANCE validation isn't passed on VAULT2");

        waitEmptyFilledOrders(vault1.getEthAddress());

        assertThat("ETH balance of vault2 (SELL ACCOUNT) didn't become more than it was", getDexBalances(vault2.getEthAddress()).getEth().get(0).getBalances().getEth(), closeTo((balanceEthVault2 + ethAmount), 0.003));
        assertThat("ETH balance of vault1 (BUY ACCOUNT) didn't become more than it was", getDexBalances(vault1.getEthAddress()).getEth().get(0).getBalances().getEth(), closeTo((balanceEthVault1 - ethAmount), 0.005));
        assertTrue(getDexBalances(vault2.getEthAddress()).getEth().get(0).getBalances().getEth() > balanceEthVault2, "ETH balance of vault2 (SELL ACCOUNT) didn't become more than it was");
        assertTrue(getDexBalances(vault1.getEthAddress()).getEth().get(0).getBalances().getEth() < (balanceEthVault1 - ethAmount), "ETH balance of vault1 (BUY ACCOUNT) didn't become less than it was ");
        assertTrue(getDexBalances(vault2.getEthAddress()).getEth().get(0).getBalances().getEth() < (balanceEthVault2 + ethAmount), "ETH balance of vault2 (SELL ACCOUNT) is more than should be! ");

        //TODO: edit ETH balance validation (should include eth comission)
        //TODO: add transaction validation on each account
        //TODO: add order status validation on each account

        //validate dex history and closed orders for account
        /*TradingDataOutputDTO dexTrades = getDexTradeInfo(true, "15");
        BigDecimal offerAmountBigDecimal = new BigDecimal(OFFER_AMOUNT);
        assertTrue(dexTrades.getV().get(0).compareTo(offerAmountBigDecimal) >= 0, "missing dex trade in trading view history");*/


        validateDexOrderResponse(sellOrderVault2.getOrder().getId(), 5, Long.valueOf(PAIR_RATE), vault2, true, true);
        validateDexOrderResponse(buyOrderVault1.getOrder().getId(), 5, Long.valueOf(PAIR_RATE), vault1, true, false);
    }

    @DisplayName("DEX EXCHANGE PAX SELL-BUY")
    @Test
    @Execution(SAME_THREAD)
    public void dexExchangePaxSellBuy() {
        //creating parameters
        final String PAIR_RATE = "1000";
        final String OFFER_AMOUNT = "5000000";
        final Long VAULT1_FEE = 900000000L; // by this dex flow it is apl fee which vault1 should pay for all dex operations
        final Long VAULT2_FEE = 800000000L; // by this dex flow it is apl fee which vault2 should pay for all dex operations
        final Long APL_AMOUNT = Long.valueOf(OFFER_AMOUNT)*100000000;
        final Long BALANCE_APL_VAULT1 = getBalance(vault1).getBalanceATM();
        final Long BALANCE_APL_VAULT2 = getBalance(vault2).getBalanceATM();
        double balancePaxVault1 = getDexBalances(vault1.getEthAddress()).getEth().get(0).getBalances().getPax();
        double balancePaxVault2 = getDexBalances(vault2.getEthAddress()).getEth().get(0).getBalances().getPax();
        double paxAmount = ((Double.valueOf(PAIR_RATE) * Double.valueOf(OFFER_AMOUNT)) * 0.000000001);

        //creating sell dex order transaction and validate response
        CreateDexOrderResponse sellOrderVault1 = createDexOrder(PAIR_RATE, OFFER_AMOUNT, vault1, false, false);
        verifyTransactionInBlock(sellOrderVault1.getOrder().getId());
        assertNotNull(sellOrderVault1, "RESPONSE is not correct/dex offer wasn't created");
        assertEquals(0, getDexOrder(sellOrderVault1.getOrder().getId()).status, "STATUS is NOT OPENED");

        //wait 25 blocks and create buy dex order and validate response
        Integer currentHeight = getBlock().getHeight();
        waitForHeight(currentHeight+25);
        CreateDexOrderResponse buyOrderVault2 = createDexOrder(PAIR_RATE, OFFER_AMOUNT, vault2, true, false);
        assertNotEquals("Exception in the process of freezing money.", buyOrderVault2.errorDescription, "Exception in the process of freezing money. Problem with ETH node.");
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
        assertEquals(BALANCE_APL_VAULT1-VAULT1_FEE-APL_AMOUNT, getBalance(vault1).getBalanceATM(), "APL BALANCE validation isn't passed on VAULT1");
        assertEquals(BALANCE_APL_VAULT2-VAULT2_FEE+APL_AMOUNT, getBalance(vault2).getBalanceATM(), "APL BALANCE validation isn't passed on VAULT2");

        waitEmptyFilledOrders(vault2.getEthAddress());

        assertEquals(balancePaxVault1 + paxAmount, getDexBalances(vault1.getEthAddress()).getEth().get(0).getBalances().getPax(), "PAX balance of vault1 (SELL ACCOUNT) isn't correct");
        assertEquals(balancePaxVault2 - paxAmount, getDexBalances(vault2.getEthAddress()).getEth().get(0).getBalances().getPax(), "PAX balance of vault2 (BUY ACCOUNT) isn't correct ");

        //TODO: edit ETH balance validation (should include eth comission)
        //TODO: add transaction validation on each account
        //TODO: add order status validation on each account

        //validate dex history and closed orders for account
        /*TradingDataOutputDTO dexTrades = getDexTradeInfo(true, "15");
        BigDecimal offerAmountBigDecimal = new BigDecimal(OFFER_AMOUNT);
        assertTrue(dexTrades.getV().get(0).compareTo(offerAmountBigDecimal) >= 0, "dex trade isn't in trading view history");*/

        validateDexOrderResponse(sellOrderVault1.getOrder().getId(), 5, Long.valueOf(PAIR_RATE), vault1, false, true);
        validateDexOrderResponse(buyOrderVault2.getOrder().getId(), 5, Long.valueOf(PAIR_RATE), vault2, false, false);
    }

    @DisplayName("DEX EXCHANGE PAX BUY-SELL")
    @Test
    @Execution(SAME_THREAD)
    public void dexExchangePaxBuySell() {
        //creating parameters
        final String pairRate = "1000";
        final String offerAmount = "4000000";
        final Long vault1Fee = 600000000L; // by this dex flow it is apl fee which vault1 should pay for all dex operations
        final Long vault2Fee = 1100000000L; // by this dex flow it is apl fee which vault2 should pay for all dex operations
        final Long aplAmount = Long.valueOf(offerAmount)*100000000;
        final Long balanceAplVault1 = getBalance(vault1).getBalanceATM();
        final Long balanceAplVault2 = getBalance(vault2).getBalanceATM();
        double balancePaxVault1 = getDexBalances(vault1.getEthAddress()).getEth().get(0).getBalances().getPax();
        double balancePaxVault2 = getDexBalances(vault2.getEthAddress()).getEth().get(0).getBalances().getPax();
        double paxAmount = ((Double.valueOf(pairRate) * Double.valueOf(offerAmount)) * 0.000000001);


        //creating BUY dex order transaction and validate response
        CreateDexOrderResponse buyOrderVault1 = createDexOrder(pairRate, offerAmount, vault1, true, false);
        assertNotEquals("Exception in the process of freezing money.", buyOrderVault1.errorDescription, "Exception in the process of freezing money. Problem with ETH node.");
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
        waitEmptyFilledOrders(vault2.getEthAddress());
        //validate APL balance + ETH balance
        assertEquals(balanceAplVault1-vault1Fee+aplAmount, getBalance(vault1).getBalanceATM(), "APL BALANCE validation isn't passed on VAULT1");
        assertEquals(balanceAplVault2-vault2Fee-aplAmount, getBalance(vault2).getBalanceATM(), "APL BALANCE validation isn't passed on VAULT2");
        log.info("EXPECTED BAlANCE PAX:");
        System.out.println(balancePaxVault2 + paxAmount);
        log.info("ACTUAL BAlANCE PAX:");
        System.out.println(getDexBalances(vault2.getEthAddress()).getEth().get(0).getBalances().getPax());

        assertEquals(balancePaxVault2 + paxAmount, getDexBalances(vault2.getEthAddress()).getEth().get(0).getBalances().getPax(), "PAX balance of vault2 (SELL ACCOUNT) isn't correct");
        assertEquals(balancePaxVault1 - paxAmount, getDexBalances(vault1.getEthAddress()).getEth().get(0).getBalances().getPax(), "PAX balance of vault1 (BUY ACCOUNT) isn't correct ");

        //TODO: edit ETH balance validation (should include eth comission)
        //TODO: add transaction validation on each account
        //TODO: add order status validation on each account
        //TODO: add trade history (closed) validation

        //validate dex history and closed orders for account
        /*TradingDataOutputDTO dexTrades = getDexTradeInfo(true, "15");
        BigDecimal offerAmountBigDecimal = new BigDecimal(offerAmount);
        assertTrue(dexTrades.getV().get(0).compareTo(offerAmountBigDecimal) >= 0, "dex trade isn't in trading view history"); */

        validateDexOrderResponse(sellOrderVault2.getOrder().getId(), 5, Long.valueOf(pairRate), vault2, false, true);
        validateDexOrderResponse(buyOrderVault1.getOrder().getId(), 5, Long.valueOf(pairRate), vault1, false, false);
    }

    @DisplayName("EXPIRED SELL APL DEX ORDER (ETH) --- > NOT IMPLEMENTED YET")
    @Test
    @Execution(SAME_THREAD)
    public void expiredSellDexEth() {

    }

    @DisplayName("EXPIRED SELL APL DEX ORDER (PAX) --- > NOT IMPLEMENTED YET")
    @Test
    @Execution(SAME_THREAD)
    public void expiredSellDexPax() {

    }

    @DisplayName("EXPIRED BUY APL DEX ORDER (ETH) --- > NOT IMPLEMENTED YET")
    @Test
    @Execution(SAME_THREAD)
    public void expiredBuyDexEth() {

    }

    @DisplayName("EXPIRED BUY APL DEX ORDER (PAX) --- > NOT IMPLEMENTED YET")
    @Test
    @Execution(SAME_THREAD)
    public void expiredBuyDexPax() {

    }

}

