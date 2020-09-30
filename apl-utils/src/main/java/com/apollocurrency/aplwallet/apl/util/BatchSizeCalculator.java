/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;

import lombok.Data;

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

    public void startTiming(int batchSize) {
        this.lastOperation = new TimeOperation(System.currentTimeMillis(), batchSize);
    }

    public void stopTiming() {
        if (lastOperation == null || lastOperation.isFinished()) {
            throw new IllegalStateException("Unable to finish timing operation, operation was not started");
        }
        long duration = lastOperation.stopTiming();
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

    public int currentBatchSize() {
        return (int) Math.max(minBatchSize, Math.min(opEMA.current(), maxBatchSize));
    }

    @Data
    static class TimeOperation {
        private final long startTime;
        private final long batchSize;
        private volatile boolean finished;

        public long stopTiming() {
            finished = true;
            return System.currentTimeMillis() - startTime;
        }

    }
}
