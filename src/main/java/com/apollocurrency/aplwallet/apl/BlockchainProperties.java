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
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class BlockchainProperties {
    private int height;
    private int maxNumberOfTransactions;
    private int blockTime;
    private long maxBalance;

    public BlockchainProperties() {
    }

    @JsonCreator
    public BlockchainProperties(
            @JsonProperty("height") int height,
            @JsonProperty("maxNumberOfTransactions") int maxNumberOfTransactions,
            @JsonProperty("blockTime") int blockTime,
            @JsonProperty("maxBalance") long maxBalance) {
        this.height = height;
        this.maxNumberOfTransactions = maxNumberOfTransactions;
        this.blockTime = blockTime;
        this.maxBalance = maxBalance;
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
