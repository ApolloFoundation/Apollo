package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.DexOrderDto;
import com.apollocurrency.aplwallet.api.response.EthGasInfoResponse;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseNew;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestDex extends TestBaseNew {

    @DisplayName("Get dex orders")
    @Test
    public void getExchangeOrders() {
        List<DexOrderDto> orders = getDexOrders();
        assertTrue(orders.size() >= 0);
    }

    @DisplayName("Get trading history for certain account")
    @Test
    public void getTradeHistory() {
        List<DexOrderDto> orders = getDexHistory(TestConfiguration.getTestConfiguration().getVaultWallet().getUser(), "1", "1");
        assertTrue(orders.size() >= 0);
    }

    @DisplayName("Get trading history for certain account")
    @Test
    public void getAllTradeHistoryByAccount() {
        List<DexOrderDto> orders = getDexHistory(TestConfiguration.getTestConfiguration().getVaultWallet().getUser());
        assertTrue(orders.size() >= 0);
    }

    @DisplayName("get gas prices for different tx speed")
    @Test
    public void getEthGasPrice(){
        EthGasInfoResponse gasPrice = getEthGas();
        assertTrue(Float.valueOf(gasPrice.fast)>=Float.valueOf(gasPrice.average));
        assertTrue(Float.valueOf(gasPrice.average)>=Float.valueOf(gasPrice.safeLow));
        assertTrue(Float.valueOf(gasPrice.safeLow)>0);
    }


}
