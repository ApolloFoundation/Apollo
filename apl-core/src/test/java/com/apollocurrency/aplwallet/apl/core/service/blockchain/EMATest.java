/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.util.EMA;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class EMATest {

    @Test
    void testEMA() {
        EMA ema = new EMA(4);
        ema.add(3.2);
        ema.add(5.8);
        ema.add(4.0);
        ema.add(2);
        double current = ema.current();
        assertEquals(3.75, current, 0.00001);
        ema.add(3.2);
        assertEquals(3.53, ema.current(), 0.00001);
    }
}