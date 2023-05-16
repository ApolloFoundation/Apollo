/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;

import lombok.Data;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class BatchSizeCalculator {
    private final int targetOperationTime;
    private final int minBatchSize;
    private final int maxBatchSize;
    private final EMA opEMA;
    private volatile TimeOperation lastOperation;
    private final double maxDecreasePercent = -0.3;
    private final double maxIncreasePercent = 0.66;



    public BatchSizeCalculator(int targetOperationTime,
                               int minBatchSize,
                               int maxBatchSize) {
        if (minBatchSize > maxBatchSize) {
            throw new IllegalArgumentException("minBatchSize should be less than maxBatchSize");
        }
        this.targetOperationTime = targetOperationTime;
        this.minBatchSize = minBatchSize;
        this.maxBatchSize = maxBatchSize;
        this.opEMA = new EMA(5);
    }

    public <T> T doTimedOp(Function<Integer, T> op, Supplier<Long> timeSupplier) {
        try {
            int batchSize = currentBatchSize();
            startTiming(timeSupplier.get(), batchSize);
            return op.apply(batchSize);
        } finally {
            stopTiming(timeSupplier.get());
        }
    }
    public void doTimedOp(Consumer<Integer> r) {
        doTimedOp((Function<Integer, Void>) batchSize -> {
            r.accept(batchSize);
            return null;
        });
    }

    public <T> T doTimedOp(Function<Integer, T> op) {
        return doTimedOp(op, System::currentTimeMillis);
    }

    public int currentBatchSize() {
        return (int) Math.max(minBatchSize, Math.min(opEMA.current(), maxBatchSize));
    }

    private void startTiming(long time, int batchSize) {
        this.lastOperation = new TimeOperation(time, batchSize);
    }

    private void stopTiming(long time) {
        if (lastOperation == null || lastOperation.isFinished()) {
            throw new IllegalStateException("Unable to finish timing operation, operation was not started");
        }
        long duration = lastOperation.stopTiming(time);
        long diff = targetOperationTime - duration;
        double percent = diff * 1.0 / targetOperationTime;
        percent /= 2;
        if (percent < 0) {
            percent = Math.max(percent, maxDecreasePercent);
        } else {
            percent = Math.min(percent, maxIncreasePercent);
        }
        int newBatchSize = (int) (lastOperation.batchSize + lastOperation.batchSize * percent);
        opEMA.add(newBatchSize);
    }

    @Data
    static class TimeOperation {
        private final long startTime;
        private final long batchSize;
        private volatile boolean finished;

        public long stopTiming(long time) {
            finished = true;
            return  time - startTime;
        }

    }
}
