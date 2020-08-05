/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

import static com.apollocurrency.aplwallet.apl.util.Constants.MAX_ENCRYPTED_MESSAGE_HEADER_LENGTH;
import static com.apollocurrency.aplwallet.apl.util.Constants.MIN_VALUE_FOR_MAX_ARBITRARY_MESSAGE_LENGTH;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"height", "maxNumberOfTransactions", "maxArbitraryMessageLength", "maxNumberOfChildAccounts", "blockTime", "maxBlockTimeLimit", "minBlockTimeLimit", "maxBalance",
    "shardingSettings", "consensusSettings", "featuresHeightRequirement"})
public class BlockchainProperties {
    @Getter
    private final int height;
    @Getter @Setter
    private int maxNumberOfTransactions;
    private int maxArbitraryMessageLength;
    @Getter
    private final int maxNumberOfChildAccounts;
    @Getter @Setter
    private int blockTime;
    @Getter
    private final int maxBlockTimeLimit;
    @Getter
    private final int minBlockTimeLimit;
    @Getter @Setter
    private long maxBalance;
    private ShardingSettings shardingSettings;
    private ConsensusSettings consensusSettings;
    private TransactionFeeSettings transactionFeeSettings;

    /**
     * Constructor for unit tests. Should not be used by JSON loading.
     *
     * @param height
     * @param maxNumberOfTransactions
     * @param maxArbitraryMessageLength
     * @param maxNumberOfChildAccounts
     * @param blockTime
     * @param maxBlockTimeLimit
     * @param minBlockTimeLimit
     * @param maxBalance
     */
    @JsonCreator
    public BlockchainProperties(
        @JsonProperty("height") int height,
        @JsonProperty("maxNumberOfTransactions") int maxNumberOfTransactions,
        @JsonProperty("maxArbitraryMessageLength") int maxArbitraryMessageLength,
        @JsonProperty("maxNumberOfChildAccount") int maxNumberOfChildAccounts,
        @JsonProperty("blockTime") int blockTime,
        @JsonProperty("maxBlockTimeLimit") int maxBlockTimeLimit,
        @JsonProperty("minBlockTimeLimit") int minBlockTimeLimit,
        @JsonProperty("maxBalance") long maxBalance) {
        this(height, maxNumberOfTransactions, maxArbitraryMessageLength, maxNumberOfChildAccounts, blockTime, maxBlockTimeLimit, minBlockTimeLimit, maxBalance, null, null);
    }

    /**
     * All fields Constructor.
     *
     * @param height
     * @param maxNumberOfTransactions
     * @param maxArbitraryMessageLength
     * @param maxNumberOfChildAccounts
     * @param blockTime
     * @param maxBlockTimeLimit
     * @param minBlockTimeLimit
     * @param maxBalance
     * @param shardingSettings
     * @param consensusSettings
     * @param transactionFeeSettings
     */
    public BlockchainProperties(
        int height,
        int maxNumberOfTransactions,
        int maxArbitraryMessageLength,
        int maxNumberOfChildAccounts,
        int blockTime,
        int maxBlockTimeLimit,
        int minBlockTimeLimit,
        long maxBalance,
        ShardingSettings shardingSettings,
        ConsensusSettings consensusSettings,
        TransactionFeeSettings transactionFeeSettings
    ) {
        this.height = height;
        this.maxNumberOfTransactions = maxNumberOfTransactions;
        this.maxArbitraryMessageLength = Math.max(MIN_VALUE_FOR_MAX_ARBITRARY_MESSAGE_LENGTH, maxArbitraryMessageLength);
        this.maxNumberOfChildAccounts = maxNumberOfChildAccounts;
        this.blockTime = blockTime;
        this.maxBlockTimeLimit = maxBlockTimeLimit;
        this.minBlockTimeLimit = minBlockTimeLimit;
        this.maxBalance = maxBalance;
        this.shardingSettings = shardingSettings == null ? new ShardingSettings() : shardingSettings;
        this.consensusSettings = consensusSettings == null ? new ConsensusSettings() : consensusSettings;
        this.transactionFeeSettings = transactionFeeSettings == null ? new TransactionFeeSettings() : transactionFeeSettings;
    }

    public BlockchainProperties(int height, int maxNumberOfTransactions, int maxArbitraryMessageLength, int blockTime, int maxBlockTimeLimit, int minBlockTimeLimit, long maxBalance, ShardingSettings shardingSettings) {
        this(height, maxNumberOfTransactions, maxArbitraryMessageLength, 1, blockTime, maxBlockTimeLimit, minBlockTimeLimit, maxBalance, shardingSettings, null, null);
    }


    public BlockchainProperties(int height, int maxNumberOfTransactions, int maxArbitraryMessageLength, int blockTime, int maxBlockTimeLimit, int minBlockTimeLimit,
                                long maxBalance, ConsensusSettings consensusSettings) {
        this(height, maxNumberOfTransactions, maxArbitraryMessageLength, 1, blockTime, maxBlockTimeLimit, minBlockTimeLimit, maxBalance, null, consensusSettings, null);
    }

    public BlockchainProperties(int height, int maxNumberOfTransactions, int maxArbitraryMessageLength, int blockTime, int maxBlockTimeLimit, int minBlockTimeLimit,
                                long maxBalance, ShardingSettings shardingSettings, ConsensusSettings consensusSettings) {
        this(height, maxNumberOfTransactions, maxArbitraryMessageLength, 1, blockTime, maxBlockTimeLimit, minBlockTimeLimit, maxBalance, shardingSettings, consensusSettings, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockchainProperties)) return false;
        BlockchainProperties that = (BlockchainProperties) o;
        return height == that.height &&
            maxNumberOfTransactions == that.maxNumberOfTransactions &&
            blockTime == that.blockTime &&
            maxBlockTimeLimit == that.maxBlockTimeLimit &&
            minBlockTimeLimit == that.minBlockTimeLimit &&
            maxBalance == that.maxBalance &&
            Objects.equals(shardingSettings, that.shardingSettings) &&
            Objects.equals(consensusSettings, that.consensusSettings) &&
            Objects.equals(transactionFeeSettings, that.transactionFeeSettings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(height, maxNumberOfTransactions, maxArbitraryMessageLength, maxNumberOfChildAccounts, blockTime, maxBlockTimeLimit, minBlockTimeLimit, maxBalance, shardingSettings, consensusSettings, transactionFeeSettings);
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getMaxNumberOfTransactions() {
        return maxNumberOfTransactions;
    }

    public void setMaxNumberOfTransactions(int maxNumberOfTransactions) {
        this.maxNumberOfTransactions = maxNumberOfTransactions;
    }

    public int getMaxArbitraryMessageLength() {
        return maxArbitraryMessageLength;
    }

    public void setMaxArbitraryMessageLength(int maxArbitraryMessageLength) {
        this.maxArbitraryMessageLength = maxArbitraryMessageLength;
    }

    public int getMaxEncryptedMessageLength() {
        return maxArbitraryMessageLength + MAX_ENCRYPTED_MESSAGE_HEADER_LENGTH;
    }

    public int getBlockTime() {
        return blockTime;
    }

    public void setBlockTime(int blockTime) {
        this.blockTime = blockTime;
    }

    public long getMaxBalance() {
        return maxBalance;
    }

    public void setMaxBalance(long maxBalance) {
        this.maxBalance = maxBalance;
    }

    public ConsensusSettings getConsensusSettings() {
        return consensusSettings;
    }

    @JsonAlias("consensus") //backward compatibility
    public void setConsensusSettings(ConsensusSettings consensusSettings) {
        this.consensusSettings = consensusSettings;
    }

    public int getMaxBlockTimeLimit() {
        return maxBlockTimeLimit;
    }

    public void setMaxBlockTimeLimit(int maxBlockTimeLimit) {
        this.maxBlockTimeLimit = maxBlockTimeLimit;
    }

    public int getMinBlockTimeLimit() {
        return minBlockTimeLimit;
    }

    public void setMinBlockTimeLimit(int minBlockTimeLimit) {
        this.minBlockTimeLimit = minBlockTimeLimit;
    }

    @JsonProperty
    public ShardingSettings getShardingSettings() {
        return shardingSettings;
    }

    @JsonSetter
    public void setShardingSettings(ShardingSettings shardingSettings) {
        this.shardingSettings = shardingSettings;
    }

    @JsonProperty
    public TransactionFeeSettings getTransactionFeeSettings() {
        return transactionFeeSettings;
    }

    @JsonSetter
    public void setTransactionFeeSettings(TransactionFeeSettings transactionFeeSettings) {
        this.transactionFeeSettings = transactionFeeSettings;
    }

    public BlockchainProperties copy() {
        return new BlockchainProperties(height, maxNumberOfTransactions, maxArbitraryMessageLength, maxNumberOfChildAccounts, blockTime, maxBlockTimeLimit, minBlockTimeLimit, maxBalance,
            shardingSettings.copy(), consensusSettings.copy(), transactionFeeSettings.copy());
    }

    @Override
    public String toString() {
        return "BlockchainProperties{" +
            "height=" + height +
            ", maxNumberOfTransactions=" + maxNumberOfTransactions +
            ", maxArbitraryMessageLength=" + maxArbitraryMessageLength +
            ", maxNumberOfChildAccount=" + maxNumberOfChildAccounts +
            ", blockTime=" + blockTime +
            ", maxBlockTimeLimit=" + maxBlockTimeLimit +
            ", minBlockTimeLimit=" + minBlockTimeLimit +
            ", maxBalance=" + maxBalance +
            ", shardingSettings=" + shardingSettings +
            ", consensusSettings=" + consensusSettings +
            ", transactionFeeSettings=" + transactionFeeSettings +
            '}';
    }
}
