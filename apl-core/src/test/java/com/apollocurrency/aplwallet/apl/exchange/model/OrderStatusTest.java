/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderStatusTest {

    @Test
    void getTypeOpen() {
        int ordinal = OrderStatus.OPEN.ordinal();

        OrderStatus result = OrderStatus.getType(ordinal);
        assertEquals(OrderStatus.OPEN, result);
    }

    @Test
    void getTypeClose() {
        int ordinal = OrderStatus.CLOSED.ordinal();

        OrderStatus result = OrderStatus.getType(ordinal);
        assertEquals(OrderStatus.CLOSED, result);
    }
}