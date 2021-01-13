/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.util.BatchSizeCalculator;
import com.apollocurrency.aplwallet.apl.util.cdi.config.Property;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.Positive;

@Singleton
public class PendingBroadcastBatchSizeCalculatorInitializer {
    private final int targetTime;
    private final int maxBatchSize;
    private final int minBatchSize;

    @Inject
    public PendingBroadcastBatchSizeCalculatorInitializer(
        @Positive @Property(name = "apl.mempool.pendingBroadcast.targetTime", defaultValue = "1000") int targetOperationTime,
        @Positive @Property(name = "apl.mempool.pendingBroadcast.minBatchSize", defaultValue = "10") int minBatchSize,
        @Positive @Property(name = "apl.mempool.pendingBroadcast.maxBatchSize", defaultValue = "255") int maxBatchSize) {
        this.targetTime = targetOperationTime;
        this.maxBatchSize = maxBatchSize;
        this.minBatchSize = minBatchSize;
    }

    @Produces
    public BatchSizeCalculator pendingBroadcastTaskBatchSizeCalculator() {
        return new BatchSizeCalculator(targetTime, minBatchSize, maxBatchSize);
    }
}
