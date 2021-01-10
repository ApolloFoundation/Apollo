/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.dex.exchange.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExchangeContractStatusTest {

    @Test
    void isStep1() {
        assertTrue(ExchangeContractStatus.STEP_1.isStep1());
        assertFalse(ExchangeContractStatus.STEP_2.isStep1());
    }

    @Test
    void isStep2() {
        assertTrue(ExchangeContractStatus.STEP_2.isStep2());
        assertFalse(ExchangeContractStatus.STEP_1.isStep2());
    }
}