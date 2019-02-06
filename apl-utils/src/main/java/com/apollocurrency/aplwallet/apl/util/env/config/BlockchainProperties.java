/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"height","maxNumberOfTransactions", "blockTime", "maxBlockTimeLimit", "minBlockTimeLimit", "maxBalance",
        "consensus"})
public class BlockchainProperties {
    private int height;
    private int maxNumberOfTransactions;
    private int blockTime;
    private int maxBlockTimeLimit;
    private int minBlockTimeLimit;
    private long maxBalance;
    private Consensus consensus;

    public BlockchainProperties() {
        this.consensus = new Consensus();
    }

    @JsonCreator
    public BlockchainProperties(
            @JsonProperty("height") int height,
            @JsonProperty("maxNumberOfTransactions") int maxNumberOfTransactions,
            @JsonProperty("blockTime") int blockTime,
            @JsonProperty("maxBlockTimeLimit") int maxBlockTimeLimit,
            @JsonProperty("minBlockTimeLimit") int minBlockTimeLimit,
            @JsonProperty("maxBalance") long maxBalance,
            @JsonProperty("consensus") Consensus consensus) {
        this();
        this.height = height;
        this.maxNumberOfTransactions = maxNumberOfTransactions;
        this.blockTime = blockTime;
        this.maxBlockTimeLimit = maxBlockTimeLimit;
        this.minBlockTimeLimit = minBlockTimeLimit;
        this.maxBalance = maxBalance;
        this.consensus = consensus;
    }

    public int getHeight() {
        return height;
    }

    public int getMaxNumberOfTransactions() {
        return maxNumberOfTransactions;
    }

    public int getBlockTime() {
        return blockTime;
    }

    public long getMaxBalance() {
        return maxBalance;
    }

    public Consensus getConsensus() {
        return consensus;
    }

    public int getMaxBlockTimeLimit() {
        return maxBlockTimeLimit;
    }

    public int getMinBlockTimeLimit() {
        return minBlockTimeLimit;
    }

    public void setConsensus(Consensus consensus) {
        this.consensus = consensus;
    }
}
