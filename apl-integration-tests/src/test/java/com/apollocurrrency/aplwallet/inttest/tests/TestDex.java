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
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Epic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import java.util.List;
import java.util.Objects;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

@DisplayName("Dex")
@Epic(value = "Dex")
@Execution(SAME_THREAD)
@ExtendWith(DexPreconditionExtension.class)
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

    }



    @DisplayName("Get dex orders")
    @Test
    public void getExchangeOrders() {
        List<DexOrderDto> orders = getDexOrders();
        assertNotNull(orders);
    }

    @DisplayName("Get trading history (closed orders) for certain account with param")
    @Test
    public void getTradeHistory() {
        List<DexOrderDto> ordersVault1 = getDexHistory(vault1.getAccountId(), true, true);
        assertNotNull(ordersVault1);
        List<DexOrderDto> ordersVault2 = getDexHistory(vault2.getAccountId(), true, true);
        assertNotNull(ordersVault2);
    }

    @DisplayName("Get trading history (closed orders) for certain account")
    @Test
    public void getAllTradeHistoryByAccount() {
        List<DexOrderDto> ordersVault1 = getDexHistory(vault1.getAccountId());
        assertNotNull(ordersVault1);
        List<DexOrderDto> ordersVault2 = getDexHistory(vault2.getAccountId());
        assertNotNull(ordersVault2);
    }

    @DisplayName("Get gas prices for different tx speed")
    @Test
    public void getEthGasPrice() {
        EthGasInfoResponse gasPrice = getEthGasInfo();
        assertTrue(Float.valueOf(gasPrice.getFast()) >= Float.valueOf(gasPrice.getAverage()));
        assertTrue(Float.valueOf(gasPrice.getAverage()) >= Float.valueOf(gasPrice.getSafeLow()));
        assertTrue(Float.valueOf(gasPrice.getSafeLow()) > 0);
    }

    @DisplayName("Obtaining ETH trading information for the given period (10 days) with certain resolution")
    @Test
    //@ParameterizedTest
    //@ValueSource(strings = {"D", "15", "60", "240"})
    public void getDexTradeInfoETH() {
        TradingDataOutputDTO dexTrades = getDexTradeInfo(true, "D");
        assertNotNull(dexTrades);
    }

    @DisplayName("Obtaining PAX trading information for the given period (10 days) with certain resolution")
    @Test
    public void getDexTradeInfoPAX() {
        TradingDataOutputDTO dexTrades = getDexTradeInfo(false, "15");
        assertNotNull(dexTrades);
    }

    @DisplayName("Create 4 types of orders and cancel them")
    @Test
    public void dexOrders() {
        log.info("Creating SELL Dex Order (ETH)");
        CreateDexOrderResponse sellOrderEth = createDexOrder("40000", "1000", vault1, false, true);
        assertNotNull(sellOrderEth, "RESPONSE is not correct/dex offer wasn't created");
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
    }

    @DisplayName("withdraw ETH/PAX + validation of ETH/PAX balances")
    @Test
    public void dexWithdrawTransactions() {
        Account2FAResponse balance = getDexBalances(TestConfiguration.getTestConfiguration().getVaultWallet().getEthAddress());
        assertNotNull(balance.getEth().get(0).getBalances().getEth());
        double ethBalance = balance.getEth().get(0).getBalances().getEth();
        assertNotNull(balance.getEth().get(0).getBalances().getPax());
        double paxBalance = balance.getEth().get(0).getBalances().getPax();

        EthGasInfoResponse gasPrice = getEthGasInfo();
        assertTrue(Float.valueOf(gasPrice.getFast()) >= Float.valueOf(gasPrice.getAverage()));
        assertTrue(Float.valueOf(gasPrice.getAverage()) >= Float.valueOf(gasPrice.getSafeLow()));
        assertTrue(Float.valueOf(gasPrice.getSafeLow()) > 0);
        Integer fastGas = Math.round(Float.valueOf(gasPrice.getFast()));
        Integer averageGas = Math.round(Float.valueOf(gasPrice.getAverage()));
        Integer safeLowGas = Math.round(Float.valueOf(gasPrice.getSafeLow()));

        //TODO: add assertion and getEthGasFee to include it into tests and validation on balances
        WithdrawResponse withdrawEth = dexWidthraw(TestConfiguration.getTestConfiguration().getVaultWallet().getEthAddress(),
                TestConfiguration.getTestConfiguration().getVaultWallet(),
                TestConfiguration.getTestConfiguration().getVaultWallet().getEthAddress(),
                "0.5",
                String.valueOf(averageGas),
                true);
        assertNotNull(withdrawEth.transactionAddress);

        //TODO: add transaction validation is accepted in ETH blockchain
        double newEthBalance = ethBalance - (21000 * 0.000000001 * averageGas);


        WithdrawResponse withdrawPax = dexWidthraw(TestConfiguration.getTestConfiguration().getVaultWallet().getEthAddress(),
                TestConfiguration.getTestConfiguration().getVaultWallet(),
                TestConfiguration.getTestConfiguration().getVaultWallet().getEthAddress(),
                "100",
                String.valueOf(fastGas),
                false);
        assertNotNull(withdrawPax.transactionAddress);
        Account2FAResponse balanceValidationPax = getDexBalances(TestConfiguration.getTestConfiguration().getVaultWallet().getEthAddress());

        //PAX balances are the same. All transaction fee is in ETH
        assertEquals(paxBalance, balanceValidationPax.getEth().get(0).getBalances().getPax(), "balances are different");
        double newEthBalanceAfterPax = newEthBalance - (300000 * 0.000000001 * fastGas);

    }

    @DisplayName("dex exchange ETH SELL-BUY")
    @Test
    public void dexExchange() {
        CreateDexOrderResponse sellOrder = createDexOrder("1000", "5000", vault1, false, true);




    }


}
