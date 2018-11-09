/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.chainid;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
@JsonPropertyOrder({"height","maxNumberOfTransactions", "blockTime", "maxBlockTimeLimit", "minBlockTimeLimit", "maxBalance", "consensus"})
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

    public static void main(String[] args) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<Chain> o = mapper.readValue(new File("conf/chains.json"), new TypeReference<List<Chain>>() {});
        Chain chain = o.get(0);
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
        ObjectWriter writer = mapper.writer(prettyPrinter);
        writer.writeValue(new File("conf/chains1.json"), o);
    }
}
