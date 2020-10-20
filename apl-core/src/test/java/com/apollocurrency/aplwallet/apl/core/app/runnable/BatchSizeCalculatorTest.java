/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.apl.util.BatchSizeCalculator;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled("Test depends on pc performance.")
class BatchSizeCalculatorTest {
    BatchSizeCalculator calculator;
    @Test
    void testEasyCalculations() {
        calculator = new BatchSizeCalculator(10, 5, 20);
        doTimeOp(11, 15);
        assertEquals(5, calculator.currentBatchSize());
        doTimeOp(8, 10);
        doTimeOp(15, 17);
        doTimeOp(5, 12);
        doTimeOp(9, 11);
        assertEquals(12, calculator.currentBatchSize());
        doTimeOp(15, 18);
        assertEquals(12, calculator.currentBatchSize());
        doTimeOp(5, 15);
        doTimeOp(7, 20);
        doTimeOp(10, 30);
        doTimeOp(9, 24);
        assertEquals(20, calculator.currentBatchSize());
    }

    @Test
    void testWideSpread() {
        calculator = new BatchSizeCalculator(20, 10, 100);
        doTimeOp(15, 22);
        doTimeOp(22, 25);
        doTimeOp(50, 35);
        doTimeOp(20, 20);
        doTimeOp(18, 20);
        assertEquals(22, calculator.currentBatchSize());
        doTimeOp(30, 25);
        assertEquals(20, calculator.currentBatchSize());
        doTimeOp(18, 24);
        doTimeOp(19, 25);
        assertEquals(23, calculator.currentBatchSize());
        doTimeOp(24, 10);
        doTimeOp(25, 8);
        doTimeOp(30, 1);
        doTimeOp(25, 2);
        assertEquals(10, calculator.currentBatchSize());

    }

    private void doTimeOp(int opTime, int batchSize) {
        calculator.startTiming(batchSize);
        ThreadUtils.sleep(opTime);
        calculator.stopTiming();
    }
}