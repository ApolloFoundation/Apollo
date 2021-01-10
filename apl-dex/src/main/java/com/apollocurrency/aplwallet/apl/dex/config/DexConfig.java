package com.apollocurrency.aplwallet.apl.dex.config;

import com.apollocurrency.aplwallet.apl.util.config.Property;
import com.google.common.base.Preconditions;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DexConfig {
    private static final int DEFAULT_DEX_MIN_TIME_OF_ATOMIC_SWAP = 2 * 60 * 60;
    private static final int DEFAULT_DEX_MAX_TIME_OF_ATOMIC_SWAP = 48 * 60 * 60;
    private static final int DEFAULT_DEX_MAX_ALLOWED_TIME_DEVIATION_FOR_ATOMIC_SWAP = 10;
    private static final int DEFAULT_DEX_MAX_ETH_ORPHAN_DEPOSIT_LIFETIME = 24 * 60 * 60;
    private static final int DEFAULT_DEX_ETH_NUMBER_OF_CONFIRMATIONS = 10; // 150 sec for 15sec blocks
    private static final int DEFAULT_DEX_APL_NUMBER_OF_CONFIRMATIONS = 30; // 150 sec for 5 sec blocks (average block time for 2/10 adaptive forging)
    private static final int MIN_ALLOWED_TIME = 60 * 5; // 5 minutes

    private final int orphanDepositLifetime;
    private final int maxAtomicSwapDuration;
    private final int minAtomicSwapDuration;
    private final int minAtomicSwapDurationWithDeviation;
    private final int maxAtomicSwapDurationWithDeviation;
    private final int deviationPercent;
    private final int ethConfirmations;
    private final int aplConfirmations;

    @Inject
    public DexConfig(
        @Property(name = "apl.dex.orderProcessor.orphanDepositLifetime", defaultValue = "" + DEFAULT_DEX_MAX_ETH_ORPHAN_DEPOSIT_LIFETIME) int orphanDepositLifetime,
        @Property(name = "apl.dex.orderProcessor.maxAtomicSwapDuration", defaultValue = "" + DEFAULT_DEX_MAX_TIME_OF_ATOMIC_SWAP) int maxAtomicSwapDuration,
        @Property(name = "apl.dex.orderProcessor.minAtomicSwapDuration", defaultValue = "" + DEFAULT_DEX_MIN_TIME_OF_ATOMIC_SWAP) int minAtomicSwapDuration,
        @Property(name = "apl.dex.orderProcessor.deviationPercent", defaultValue = "" + DEFAULT_DEX_MAX_ALLOWED_TIME_DEVIATION_FOR_ATOMIC_SWAP) int deviationPercent,
        @Property(name = "apl.dex.orderProcessor.ethConfirmations", defaultValue = "" + DEFAULT_DEX_ETH_NUMBER_OF_CONFIRMATIONS) int ethConfirmations,
        @Property(name = "apl.dex.orderProcessor.aplConfirmations", defaultValue = "" + DEFAULT_DEX_APL_NUMBER_OF_CONFIRMATIONS) int aplConfirmations
    ) {
        Preconditions.checkArgument(maxAtomicSwapDuration / 2 > minAtomicSwapDuration, "Max atomic swap duration should be at least 2 times greater than min atomic swap duration");
        Preconditions.checkArgument(maxAtomicSwapDuration >= MIN_ALLOWED_TIME, "Max atomic swap duration should be greater than " + MIN_ALLOWED_TIME);
        Preconditions.checkArgument(minAtomicSwapDuration >= MIN_ALLOWED_TIME, "Min atomic swap duration should be greater than " + MIN_ALLOWED_TIME);
        Preconditions.checkArgument(orphanDepositLifetime >= MIN_ALLOWED_TIME, "Orphan deposit lifetime should be greater than " + MIN_ALLOWED_TIME);
        this.orphanDepositLifetime = orphanDepositLifetime;
        this.maxAtomicSwapDuration = maxAtomicSwapDuration;
        this.minAtomicSwapDuration = minAtomicSwapDuration;
        this.deviationPercent = Math.max(deviationPercent, 1);
        this.ethConfirmations = Math.max(ethConfirmations, 1);
        this.aplConfirmations = Math.max(aplConfirmations, 1);
        this.minAtomicSwapDurationWithDeviation = this.minAtomicSwapDuration - (this.minAtomicSwapDuration * this.deviationPercent) / 100;
        this.maxAtomicSwapDurationWithDeviation = this.maxAtomicSwapDuration + (this.maxAtomicSwapDuration * this.deviationPercent) / 100;
    }

    public int getOrphanDepositLifetime() {
        return orphanDepositLifetime;
    }

    public int getMaxAtomicSwapDuration() {
        return maxAtomicSwapDuration;
    }

    public int getMinAtomicSwapDurationWithDeviation() {
        return minAtomicSwapDurationWithDeviation;
    }

    public int getMaxAtomicSwapDurationWithDeviation() {
        return maxAtomicSwapDurationWithDeviation;
    }

    public int getMinAtomicSwapDuration() {
        return minAtomicSwapDuration;
    }

    public int getDeviationPercent() {
        return deviationPercent;
    }

    public int getEthConfirmations() {
        return ethConfirmations;
    }

    public int getAplConfirmations() {
        return aplConfirmations;
    }
}
