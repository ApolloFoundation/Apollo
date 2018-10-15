/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
public class Chain {
    private UUID chainId;
    private boolean isTestnet;
    private List<String> defaultPeers;
    private List<String> wellKnownPeers;
    private List<String> blacklistedPeers;
    private String name;
    private String description;
    private String symbol;
    private String prefix;
    private String project;
    private List<BlockchainProperties> blockchainProperties;
    @JsonCreator
    public Chain(@JsonProperty("chainId") UUID chainId,
                 @JsonProperty("isTestnet") boolean isTestnet,
                 @JsonProperty("defaultPeers") List<String> defaultPeers,
                 @JsonProperty("wellKnownPeers") List<String> wellKnownPeers,
                 @JsonProperty("blacklistedPeers") List<String> blacklistedPeers,
                 @JsonProperty("name") String name,
                 @JsonProperty("description") String description,
                 @JsonProperty("symbol") String symbol,
                 @JsonProperty("prefix") String prefix,
                 @JsonProperty("project") String project,
                 @JsonProperty("blockchainProperties") List<BlockchainProperties> blockchainProperties
    ) {
        this.chainId = chainId;
        this.isTestnet = isTestnet;
        this.defaultPeers = defaultPeers;
        this.wellKnownPeers = wellKnownPeers;
        this.blacklistedPeers = blacklistedPeers;
        this.name = name;
        this.description = description;
        this.symbol = symbol;
        this.prefix = prefix;
        this.project = project;
        this.blockchainProperties = Collections.unmodifiableList(blockchainProperties);
    }

    public UUID getChainId() {
        return chainId;
    }

    public boolean isTestnet() {
        return isTestnet;
    }

    public List<String> getDefaultPeers() {
        return defaultPeers;
    }

    public List<String> getWellKnownPeers() {
        return wellKnownPeers;
    }

    public List<String> getBlacklistedPeers() {
        return blacklistedPeers;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getProject() {
        return project;
    }

    public List<BlockchainProperties> getBlockchainProperties() {
        return blockchainProperties;
    }
}
