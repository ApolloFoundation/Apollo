/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.chainid;

import com.apollocurrency.aplwallet.apl.util.Constants;

/**
 * This class provide scaled by block time config values. Should be used in future for {@link BlockchainConfig}
 * instead of hardcoded values
 */
public class BlockTimeScaledConfig {
    private final int leasingDelay;
    private final int guaranteedBalanceConfirmations;
    private final int referencedTransactionHeightSpan;

    public BlockTimeScaledConfig(int blockTime) {
        this.leasingDelay = (int) (1440 * (60 / (double)blockTime));
        this.guaranteedBalanceConfirmations = (int) (1440 * (60 / (double)blockTime));
        this.referencedTransactionHeightSpan = Constants.MAX_REFERENCED_TRANSACTION_TIMESPAN / blockTime;
    }

    public int getReferencedTransactionHeightSpan() {
        return referencedTransactionHeightSpan;
    }

    public int getLeasingDelay() {
        return leasingDelay;
    }

    public int getGuaranteedBalanceConfirmations() {
        return guaranteedBalanceConfirmations;
    }
}
