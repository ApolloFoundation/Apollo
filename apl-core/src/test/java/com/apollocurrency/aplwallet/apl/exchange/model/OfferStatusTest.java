package com.apollocurrency.aplwallet.apl.exchange.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OfferStatusTest {

    @Test
    void getTypeOpen() {
        int ordinal = OfferStatus.OPEN.ordinal();

        OfferStatus result = OfferStatus.getType(ordinal);
        assertEquals(OfferStatus.OPEN, result);
    }

    @Test
    void getTypeClose() {
        int ordinal = OfferStatus.CLOSED.ordinal();

        OfferStatus result = OfferStatus.getType(ordinal);
        assertEquals(OfferStatus.CLOSED, result);
    }
}