/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.chainid;

import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.env.config.BlockchainProperties;
import com.apollocurrency.aplwallet.apl.util.env.config.ConsensusSettings;

import java.math.BigInteger;
import java.util.Objects;

public class HeightConfig {
    private final BlockchainProperties bp;
    private final BlockTimeScaledConfig blockTimeScaledConfig;
    private final int maxPayloadLength;
    private final long maxBalanceAtm;
    private final long initialBaseTarget;
    private final long maxBaseTarget;
    private final long minBaseTarget;

    public HeightConfig(BlockchainProperties bp, long oneAPL, long initialSupply) {
        this.bp = Objects.requireNonNull(bp, "Blockchain properties cannot be null");
        this.maxPayloadLength = bp.getMaxNumberOfTransactions() * Constants.MIN_TRANSACTION_SIZE;
        if (bp.getMaxBalance() > initialSupply) {
            throw new IllegalArgumentException("Wrong height config, height=" + bp.getHeight() + ". The maxBalanceATM value " + bp.getMaxBalance() + " can't be greater than the initialSupply value " + initialSupply);
        }
        this.maxBalanceAtm = Math.multiplyExact(bp.getMaxBalance(), oneAPL);
        this.initialBaseTarget = BigInteger.valueOf(2).pow(63).divide(BigInteger.valueOf(bp.getBlockTime() * bp.getMaxBalance())).longValue();
        this.maxBaseTarget = initialBaseTarget * 50;
        this.minBaseTarget = initialBaseTarget * 9 / 10;
        this.blockTimeScaledConfig = new BlockTimeScaledConfig(bp.getBlockTime());
        if (bp.getMaxBlockTimeLimit() < bp.getMinBlockTimeLimit()) {
            throw new IllegalArgumentException("maxBlockTimeLimit '" + bp.getMaxBlockTimeLimit() + "' is less than minBlockTimeLimit '" + bp.getMinBlockTimeLimit()+"'");
        }
        if (bp.getMaxBlockTimeLimit() < bp.getBlockTime()) {
            throw new IllegalArgumentException("maxBlockTimeLimit '" + bp.getMaxBlockTimeLimit() + "' is less than blockTime '" + bp.getBlockTime() + "'");
        }
        if (bp.getMinBlockTimeLimit() > bp.getBlockTime()) {
            throw new IllegalArgumentException("minBlockTimeLimit '" + bp.getMinBlockTimeLimit() + "' is greater than blockTime '" + bp.getBlockTime() + "'");
        }
    }

    public int getReferencedTransactionHeightSpan() {
        return blockTimeScaledConfig.getReferencedTransactionHeightSpan();
    }

    public int getMaxPayloadLength() {
        return maxPayloadLength;
    }

    public long getMaxBalanceATM() {
        return maxBalanceAtm;
    }

    public long getInitialBaseTarget() {
        return initialBaseTarget;
    }

    public long getMaxBaseTarget() {
        return maxBaseTarget;
    }

    public long getMinBaseTarget() {
        return minBaseTarget;
    }

    public int getHeight() {
        return bp.getHeight();
    }

    public int getMaxNumberOfTransactions() {
        return bp.getMaxNumberOfTransactions();
    }

    public int getMaxArbitraryMessageLength() {
        return bp.getMaxArbitraryMessageLength();
    }

    public int getMaxEncryptedMessageLength() {
        return bp.getMaxEncryptedMessageLength();
    }

    public int getMaxNumberOfChildAccount() {
        return bp.getMaxNumberOfChildAccounts();
    }

    public int getBlockTime() {
        return bp.getBlockTime();
    }

    public long getMaxBalanceAPL() {
        return bp.getMaxBalance();
    }

    public int getMaxBlockTimeLimit() {
        return bp.getMaxBlockTimeLimit();
    }

    public int getMinBlockTimeLimit() {
        return bp.getMinBlockTimeLimit();
    }

    public String getShardingDigestAlgorithm() {
        return bp.getShardingSettings().getDigestAlgorithm();
    }

    public ConsensusSettings.Type getConsensusType() {
        return bp.getConsensusSettings().getType();
    }

    public int getAdaptiveBlockTime() {
        return bp.getConsensusSettings().getAdaptiveForgingSettings().getAdaptiveBlockTime();
    }

    public int getNumberOfTransactionsInAdaptiveBlock() {
        return bp.getConsensusSettings().getAdaptiveForgingSettings().getNumberOfTransactions();
    }

    public boolean isAdaptiveForgingEnabled() {
        return bp.getConsensusSettings().getAdaptiveForgingSettings().isEnabled();
    }

    public boolean isShardingEnabled() {
        return bp.getShardingSettings().isEnabled();
    }

    public int getShardingFrequency() {
        return bp.getShardingSettings().getFrequency();
    }

    public short getFeeRate(byte type, byte subType) {
        return bp.getTransactionFeeSettings().getRate(type, subType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HeightConfig)) return false;
        HeightConfig that = (HeightConfig) o;
        return maxPayloadLength == that.maxPayloadLength &&
            maxBalanceAtm == that.maxBalanceAtm &&
            initialBaseTarget == that.initialBaseTarget &&
            maxBaseTarget == that.maxBaseTarget &&
            minBaseTarget == that.minBaseTarget &&
            Objects.equals(bp, that.bp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bp, maxPayloadLength, maxBalanceAtm, initialBaseTarget, maxBaseTarget, minBaseTarget);
    }

    @Override
    public String toString() {
        return "HeightConfig{" +
            "bp=" + bp +
            ", maxPayloadLength=" + maxPayloadLength +
            ", maxBalanceAtm=" + maxBalanceAtm +
            ", initialBaseTarget=" + initialBaseTarget +
            ", maxBaseTarget=" + maxBaseTarget +
            ", minBaseTarget=" + minBaseTarget +
            '}';
    }
}
