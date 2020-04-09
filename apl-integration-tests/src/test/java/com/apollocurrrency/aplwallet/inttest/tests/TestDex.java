package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.DexOrderDto;
import com.apollocurrency.aplwallet.api.dto.DexTradeInfoDto;
import com.apollocurrency.aplwallet.api.response.Account2FAResponse;
import com.apollocurrency.aplwallet.api.response.EthGasInfoResponse;
import com.apollocurrency.aplwallet.api.response.WithdrawResponse;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseNew;
import io.qameta.allure.Epic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Dex")
@Epic(value = "Dex")
public class TestDex extends TestBaseNew {

    @DisplayName("Get dex orders")
    @Test
    public void getExchangeOrders() {
        List<DexOrderDto> orders = getDexOrders();
        assertNotNull(orders);
    }

    @DisplayName("Get trading history for certain account with param")
    @Test
    public void getTradeHistory() {
        List<DexOrderDto> orders = getDexHistory(TestConfiguration.getTestConfiguration().getVaultWallet().getUser(), "1", "1");
        assertNotNull(orders);
    }

    @DisplayName("Get trading history for certain account")
    @Test
    public void getAllTradeHistoryByAccount() {
        List<DexOrderDto> orders = getDexHistory(TestConfiguration.getTestConfiguration().getVaultWallet().getUser());
        assertNotNull(orders);
    }

    @DisplayName("Get gas prices for different tx speed")
    @Test
    public void getEthGasPrice() {
        EthGasInfoResponse gasPrice = getEthGasInfo();
        assertTrue(Float.valueOf(gasPrice.getFast()) >= Float.valueOf(gasPrice.getAverage()));
        assertTrue(Float.valueOf(gasPrice.getAverage()) >= Float.valueOf(gasPrice.getSafeLow()));
        assertTrue(Float.valueOf(gasPrice.getSafeLow()) > 0);
    }

    @DisplayName("Obtaining trading information for the given period")
    @Test
    public void getDexTradeInfoETH() {
        List<DexTradeInfoDto> dexTrades = getDexTradeInfo("1", 0, 999999999);
        assertNotNull(dexTrades);
    }

    @DisplayName("Obtaining trading information for the given period")
    @Test
    public void getDexTradeInfoPAX() {
        List<DexTradeInfoDto> dexTrades = getDexTradeInfo("2", 0, 999999999);
        assertNotNull(dexTrades);
    }

    @DisplayName("Create 4 types of orders and cancel them")
    @Test
    public void dexOrders() {
        //Create Sell order ETH
        String sellOrderEth = createDexOrder("40000", "1000", TestConfiguration.getTestConfiguration().getVaultWallet(), false, true);
        assertEquals("{}", sellOrderEth, "dex offer wasn't created");
        //Create Sell order PAX
        String sellOrderPax = createDexOrder("40000", "1000", TestConfiguration.getTestConfiguration().getVaultWallet(), false, false);
        assertEquals("{}", sellOrderPax, "dex offer wasn't created");

        //Create Buy order PAX
        String buyOrderPax = createDexOrder("15000", "1000", TestConfiguration.getTestConfiguration().getVaultWallet(), true, false);
        assertNotNull(buyOrderPax);
        //Create Buy order ETH
        String buyOrderEth = createDexOrder("15000", "1000", TestConfiguration.getTestConfiguration().getVaultWallet(), true, true);
        assertNotNull(buyOrderEth);

        List<DexOrderDto> orders = getDexOrders(TestConfiguration.getTestConfiguration().getVaultWallet().getAccountId());
        //TODO: add additional asserts for checking statuses after order was cancelled
        for (DexOrderDto order : orders) {
            if (order.status == 0) {
                verifyCreatingTransaction(dexCancelOrder(order.id, TestConfiguration.getTestConfiguration().getVaultWallet()));
            }
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
        //System.out.println(newEthBalance);
        //Account2FAResponse balanceValidationEth = getDexBalances(TestConfiguration.getTestConfiguration().getVaultWallet().getEthAddress());
        //assertEquals(newEthBalance, balanceValidationEth.getEth().get(0).getBalances().getEth(), "balances are different");

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
        //System.out.println(newEthBalance);
        //Account2FAResponse balanceValidationEthAfterPax = getDexBalances(TestConfiguration.getTestConfiguration().getVaultWallet().getEthAddress());
        //assertEquals(newEthBalanceAfterPax, balanceValidationEthAfterPax.getEth().get(0).getBalances().getEth(), "balances are different");
    }


}
