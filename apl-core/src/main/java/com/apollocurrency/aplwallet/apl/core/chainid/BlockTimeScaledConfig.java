/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.chainid;

/**
 * This class provide scaled by block time config values. Should be used in future for {@link BlockchainConfig}
 * instead of hardcoded values
 */
public class BlockTimeScaledConfig {
    private final int leasingDelay;
    private final int guaranteedBalanceConfirmations;

    public BlockTimeScaledConfig(int blockTime) {
        this.leasingDelay = (int) (1440 * (60 / (double)blockTime));
        this.guaranteedBalanceConfirmations = (int) (1440 * (60 / (double)blockTime));
    }

    public int getLeasingDelay() {
        return leasingDelay;
    }

    public int getGuaranteedBalanceConfirmations() {
        return guaranteedBalanceConfirmations;
    }
}
