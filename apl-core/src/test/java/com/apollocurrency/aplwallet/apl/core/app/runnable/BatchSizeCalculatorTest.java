/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.apl.util.BatchSizeCalculator;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BatchSizeCalculatorTest {
    BatchSizeCalculator calculator;
    @Test
    void doTimedOp_consumer() {
        calculator = new BatchSizeCalculator(10, 5, 20);
        calculator.doTimedOp((batchSize)-> {
            assertEquals(5, batchSize);
        });
        assertEquals(5, calculator.currentBatchSize());
    }

    @Test
    void doTimedOp_function() {
        calculator = new BatchSizeCalculator(10, 5, 20);
        Integer result = calculator.doTimedOp((batchSize) -> {
            assertEquals(5, batchSize);
            return 2;
        });
        assertEquals(5, calculator.currentBatchSize());
        assertEquals(2, result);
    }

    @Test
    void testManyIterations() {
        calculator = new BatchSizeCalculator(20, 10, 100);
        doTimeOp(0, 15);
        doTimeOp(15, 20);
        doTimeOp(40, 50);
        doTimeOp(100, 120);
        doTimeOp(120, 118);
        assertEquals(12, calculator.currentBatchSize());

        doTimeOp(140, 160);
        assertEquals(12, calculator.currentBatchSize());

        speedup(5);
        assertEquals(24, calculator.currentBatchSize());

        speedup(9);
        assertEquals(92, calculator.currentBatchSize());

        speedup(10);
        assertEquals(100, calculator.currentBatchSize());

        doTimeOp(230, 300);
        doTimeOp(300, 400);
        doTimeOp(400, 415);
        doTimeOp(415, 475);
        doTimeOp(515, 575);
        assertEquals(84, calculator.currentBatchSize());
    }

    private void speedup(int iterations) {
        for (int i = 0; i < iterations; i++) {
            doTimeOp(0, 0);
        }
    }

    private void doTimeOp(long startTime, long finishTIme) {
        calculator.doTimedOp(batchSize -> null, new Supplier<>() {
            int counter = 0;
            @Override
            public Long get() {
                if (++counter == 1) {
                    return startTime;
                } else if (counter == 2) {
                    return finishTIme;
                } else {
                    throw new IllegalStateException("Too many calls to the mock time supplier, only two calls allowed for the beginning of the operation and its end");
                }
            }
        });
    }
}