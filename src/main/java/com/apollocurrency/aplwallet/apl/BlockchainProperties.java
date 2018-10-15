/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
public class BlockchainProperties {
    private int height;
    private int maxNumberOfTransactions;
    private int blockTime;
    private long maxBalance;
    private String genesisLocation;

    public BlockchainProperties() {
    }

    @JsonCreator
    public BlockchainProperties(
            @JsonProperty("height") int height,
            @JsonProperty("maxNumberOfTransactions") int maxNumberOfTransactions,
            @JsonProperty("blockTime") int blockTime,
            @JsonProperty("maxBalance") long maxBalance,
            @JsonProperty("genesisLocation") String genesisLocation) {
        this.height = height;
        this.maxNumberOfTransactions = maxNumberOfTransactions;
        this.blockTime = blockTime;
        this.maxBalance = maxBalance;
        this.genesisLocation = genesisLocation;
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

    public String getGenesisLocation() {
        return genesisLocation;
    }

    public static void main(String[] args) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<Chain> o = mapper.readValue(new File("conf/chains.json"), new TypeReference<List<Chain>>() {});
        Chain chain = o.get(0);
    }
}
