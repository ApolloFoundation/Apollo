/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.apl.util.BatchSizeCalculator;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BatchSizeCalculatorTest {
    BatchSizeCalculator calculator;
    @Test
    void testEasyCalculations() {
        calculator = new BatchSizeCalculator(10, 5, 20);
        doTimeOp(0, 11, 15);
        assertEquals(5, calculator.currentBatchSize());
        doTimeOp(11, 19, 10);
        doTimeOp(19, 34, 17);
        doTimeOp(34, 39, 12);
        doTimeOp(39, 48, 11);
        assertEquals(12, calculator.currentBatchSize());
        doTimeOp(48, 63, 18);
        assertEquals(12, calculator.currentBatchSize());
        doTimeOp(63, 68, 15);
        doTimeOp(68, 75, 20);
        doTimeOp(75, 85, 30);
        doTimeOp(85, 94, 24);
        assertEquals(20, calculator.currentBatchSize());
    }

    @Test
    void testWideSpread() {
        calculator = new BatchSizeCalculator(20, 10, 100);
        doTimeOp(0, 15, 22);
        doTimeOp(15, 37, 25);
        doTimeOp(40, 90, 35);
        doTimeOp(100, 120, 20);
        doTimeOp(120, 138, 20);
        assertEquals(22, calculator.currentBatchSize());
        doTimeOp(140, 170, 25);
        assertEquals(20, calculator.currentBatchSize());
        doTimeOp(170, 188, 24);
        doTimeOp(188, 207, 25);
        assertEquals(207, 230, calculator.currentBatchSize());
        doTimeOp(230, 254, 10);
        doTimeOp(254, 279, 8);
        doTimeOp(279, 309, 1);
        doTimeOp(309, 334, 2);
        assertEquals(10, calculator.currentBatchSize());

    }

    private void doTimeOp(long startTime, long finishTIme, int batchSize) {
        calculator.startTiming(startTime, batchSize);
        ThreadUtils.sleep(finishTIme - startTime);
        calculator.stopTiming(finishTIme);
    }
}