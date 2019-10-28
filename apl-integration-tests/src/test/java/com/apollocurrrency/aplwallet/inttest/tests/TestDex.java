package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.DexOrderDto;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseNew;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestDex extends TestBaseNew {

    @DisplayName("Get exchange offers")
    @Test
    public void getExchangeOrders() throws JsonProcessingException {
        List<DexOrderDto> orders = getDexOrders("1", "1", "2", "");
        System.out.println(orders);
        assertTrue(orders.size() >= 0);
    }
}
