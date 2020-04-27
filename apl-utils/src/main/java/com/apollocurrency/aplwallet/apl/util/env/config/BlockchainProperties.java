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

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"height", "maxNumberOfTransactions", "blockTime", "maxBlockTimeLimit", "minBlockTimeLimit", "maxBalance",
    "shardingSettings", "consensusSettings", "featuresHeightRequirement"})
public class BlockchainProperties {
    private int height;
    private int maxNumberOfTransactions;
    private int blockTime;
    private int maxBlockTimeLimit;
    private int minBlockTimeLimit;
    private long maxBalance;
    private ShardingSettings shardingSettings;
    private ConsensusSettings consensusSettings;

//    @JsonCreator
    public BlockchainProperties(
        @JsonProperty("height") int height,
        @JsonProperty("maxNumberOfTransactions") int maxNumberOfTransactions,
        @JsonProperty("blockTime") int blockTime,
        @JsonProperty("maxBlockTimeLimit") int maxBlockTimeLimit,
        @JsonProperty("minBlockTimeLimit") int minBlockTimeLimit,
        @JsonProperty("maxBalance") long maxBalance) {
        this(height, maxNumberOfTransactions, blockTime, maxBlockTimeLimit, minBlockTimeLimit, maxBalance, null, null);
    }

    @JsonCreator
    public BlockchainProperties(
        @JsonProperty("height") int height,
        @JsonProperty("maxNumberOfTransactions") int maxNumberOfTransactions,
        @JsonProperty("blockTime") int blockTime,
        @JsonProperty("maxBlockTimeLimit") int maxBlockTimeLimit,
        @JsonProperty("minBlockTimeLimit") int minBlockTimeLimit,
        @JsonProperty("maxBalance") long maxBalance,
        @JsonProperty("shardingSettings") ShardingSettings shardingSettings,
        /*@JsonProperty("consensusSettings") */ConsensusSettings consensusSettings) {
        this.height = height;
        this.maxNumberOfTransactions = maxNumberOfTransactions;
        this.blockTime = blockTime;
        this.maxBlockTimeLimit = maxBlockTimeLimit;
        this.minBlockTimeLimit = minBlockTimeLimit;
        this.maxBalance = maxBalance;
        this.shardingSettings = shardingSettings == null ? new ShardingSettings() : shardingSettings;
        this.shardingSettings.setStartHeight(height); // needed for unit tests mostly
        this.consensusSettings = consensusSettings == null ? new ConsensusSettings() : consensusSettings;
    }

    public BlockchainProperties(int height, int maxNumberOfTransactions, int blockTime, int maxBlockTimeLimit, int minBlockTimeLimit, long maxBalance, ShardingSettings shardingSettings) {
        this(height, maxNumberOfTransactions, blockTime, maxBlockTimeLimit, minBlockTimeLimit, maxBalance, shardingSettings, null);
    }


    public BlockchainProperties(int height, int maxNumberOfTransactions, int blockTime, int maxBlockTimeLimit, int minBlockTimeLimit,
                                long maxBalance, ConsensusSettings consensusSettings) {
        this(height, maxNumberOfTransactions, blockTime, maxBlockTimeLimit, minBlockTimeLimit, maxBalance, null, consensusSettings);
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
            Objects.equals(consensusSettings, that.consensusSettings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(height, maxNumberOfTransactions, blockTime, maxBlockTimeLimit, minBlockTimeLimit, maxBalance, shardingSettings, consensusSettings);
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

    @JsonProperty
    public void setShardingSettings(ShardingSettings shardingSettings) {
        this.shardingSettings = shardingSettings;
    }

    public BlockchainProperties copy() {
        return new BlockchainProperties(height, maxNumberOfTransactions, blockTime, maxBlockTimeLimit, minBlockTimeLimit, maxBalance,
            shardingSettings.copy(), consensusSettings.copy());
    }

    @Override
    public String toString() {
        return "BlockchainProperties{" +
            "height=" + height +
            ", maxNumberOfTransactions=" + maxNumberOfTransactions +
            ", blockTime=" + blockTime +
            ", maxBlockTimeLimit=" + maxBlockTimeLimit +
            ", minBlockTimeLimit=" + minBlockTimeLimit +
            ", maxBalance=" + maxBalance +
            ", shardingSettings=" + shardingSettings +
            ", consensusSettings=" + consensusSettings +
            '}';
    }
}
