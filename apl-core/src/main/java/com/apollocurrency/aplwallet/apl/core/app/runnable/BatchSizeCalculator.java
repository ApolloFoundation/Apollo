/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.EMA;
import lombok.Data;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.Positive;

@Singleton
public class BatchSizeCalculator {
    private final int targetOperationTime;
    private final int minBatchSize;
    private final int maxBatchSize;
    private final EMA opEMA;
    private volatile TimeOperation lastOperation;


    @Inject
    public BatchSizeCalculator(@Positive @Property(name = "apl.pendingBroadcast.targetTime", defaultValue = "1000") int targetOperationTime,
                               @Positive @Property(name = "apl.pendingBroadcast.minBatchSize", defaultValue = "10") int minBatchSize,
                               @Positive @Property(name = "apl.pendingBroadcast.maxBatchSize", defaultValue = "255") int maxBatchSize) {
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
