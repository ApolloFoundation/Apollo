/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.chainid;

import java.math.BigInteger;

import com.apollocurrency.aplwallet.apl.Constants;

public class HeightConfig {
    private final int maxNumberOfTransactions;
    private final int maxPayloadLength;
    private final long maxBalanceApl;
    private final long maxBalanceAtm;
    private final int blockTime;
    private final long initialBaseTarget;
    private final long maxBaseTarget;
    private final long minBaseTarget;
    private final int minBlockTimeLimit;
    private final int maxBlockTimeLimit;
    private final boolean isAdaptiveForgingEnabled;
    private final int adaptiveBlockTime;
    private final Consensus.Type consensusType;
    private final int numberOfTransactionsInAdaptiveBlock;
    public HeightConfig(BlockchainProperties bp, boolean testnet) {
        this.maxNumberOfTransactions = bp.getMaxNumberOfTransactions();
        this.maxBalanceApl = bp.getMaxBalance();
        this.blockTime = bp.getBlockTime();
        this.maxPayloadLength = maxNumberOfTransactions * Constants.MIN_TRANSACTION_SIZE;
        this.maxBalanceAtm = maxBalanceApl * Constants.ONE_APL;
        this.initialBaseTarget = BigInteger.valueOf(2).pow(63).divide(BigInteger.valueOf(blockTime * maxBalanceApl)).longValue();
        this.maxBaseTarget = initialBaseTarget * (testnet ? maxBalanceApl : 50);
        this.minBaseTarget = initialBaseTarget * 9 / 10;
        this.minBlockTimeLimit = bp.getMinBlockTimeLimit();
        this.maxBlockTimeLimit = bp.getMaxBlockTimeLimit();
        AdaptiveForgingSettings adaptiveForgingSettings = bp.getConsensus().getAdaptiveForgingSettings();
        this.isAdaptiveForgingEnabled = adaptiveForgingSettings.isEnabled();
        this.adaptiveBlockTime = adaptiveForgingSettings.getAdaptiveBlockTime();
        this.numberOfTransactionsInAdaptiveBlock = adaptiveForgingSettings.getNumberOfTransactions();
        this.consensusType = bp.getConsensus().getType();
    }

    public int getMaxNumberOfTransactions() {
        return maxNumberOfTransactions;
    }

    public int getMaxPayloadLength() {
        return maxPayloadLength;
    }

    public long getMaxBalanceAPL() {
        return maxBalanceApl;
    }

    public long getMaxBalanceATM() {
        return maxBalanceAtm;
    }

    public int getBlockTime() {
        return blockTime;
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

    public int getMinBlockTimeLimit() {
        return minBlockTimeLimit;
    }

    public int getMaxBlockTimeLimit() {
        return maxBlockTimeLimit;
    }

    public boolean isAdaptiveForgingEnabled() {
        return isAdaptiveForgingEnabled;
    }

    public int getAdaptiveBlockTime() {
        return adaptiveBlockTime;
    }

    public Consensus.Type getConsensusType() {
        return consensusType;
    }

    public int getNumberOfTransactionsInAdaptiveBlock() {
        return numberOfTransactionsInAdaptiveBlock;
    }

    @Override
    public String toString() {
        return "HeightConfig{" +
                "blockTime=" + blockTime +
                ", initialBaseTarget=" + initialBaseTarget +
                ", minBlockTimeLimit=" + minBlockTimeLimit +
                ", maxBlockTimeLimit=" + maxBlockTimeLimit +
                ", isAdaptiveForgingEnabled=" + isAdaptiveForgingEnabled +
                ", adaptiveBlockTime=" + adaptiveBlockTime +
                ", consensusType=" + consensusType +
                ", numberOfTransactionsInAdaptiveBlock=" + numberOfTransactionsInAdaptiveBlock +
                '}';
    }
}
