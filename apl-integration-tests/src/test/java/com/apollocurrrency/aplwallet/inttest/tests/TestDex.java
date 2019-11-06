package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.DexOrderDto;
import com.apollocurrency.aplwallet.api.dto.DexTradeInfoDto;
import com.apollocurrency.aplwallet.api.response.EthGasInfoResponse;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseNew;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@DisplayName("Dex")
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
    public void getEthGasPrice(){
        EthGasInfoResponse gasPrice = getEthGasInfo();
        assertTrue(Float.valueOf(gasPrice.getFast())>=Float.valueOf(gasPrice.getAverage()));
        assertTrue(Float.valueOf(gasPrice.getAverage())>= Float.valueOf(gasPrice.getSafeLow()));
        assertTrue(Float.valueOf(gasPrice.getSafeLow())>0);
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

}
