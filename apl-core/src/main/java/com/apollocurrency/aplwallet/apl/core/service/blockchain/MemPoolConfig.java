/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.util.cdi.config.Property;
import lombok.Getter;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Getter
public class MemPoolConfig {
    private final boolean enableRebroadcasting;
    private final int maxUnconfirmedTransactions;
    private final int maxCachedTransactions;
    private final int maxPendingTransactions;
    private final int maxReferencedTxs;
    private final int processLaterQueueSize;


    @Inject
    public MemPoolConfig(@Property(name = "apl.maxUnconfirmedTransactions", defaultValue = "" + Integer.MAX_VALUE) int maxUnconfirmedTransactions,
                         @Property(name = "apl.mempool.maxCachedTransactions", defaultValue = "2000") int maxCachedTransactions,
                         @Property(name = "apl.enableTransactionRebroadcasting") boolean enableRebroadcasting,
                         @Property(name = "apl.mempool.maxPendingTransactions", defaultValue = "3000") int maxPendingTransactions,
                         @Property(name = "apl.mempool.processLaterQueueSize", defaultValue = "5000") int processLaterQueueSize,
                         @Property(name = "apl.mempool.maxReferencedTransactions", defaultValue = "100") int maxReferencedTxs
                         ) {
        this.maxCachedTransactions = maxCachedTransactions;
        this.enableRebroadcasting = enableRebroadcasting;
        this.maxUnconfirmedTransactions = maxUnconfirmedTransactions;
        this.maxReferencedTxs = maxReferencedTxs;
        this.maxPendingTransactions = maxPendingTransactions;
        this.processLaterQueueSize = processLaterQueueSize;
    }
}
