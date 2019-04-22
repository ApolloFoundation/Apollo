/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@JsonPropertyOrder({"chainId", "active", "defaultPeers", "wellKnownPeers", "blacklistedPeers", "name", "description", "symbol",
        "prefix", "project", "genesisLocation", "blockchainProperties"})
public class Chain {
    private UUID chainId;
    private boolean active;
    private List<String> defaultPeers;
    private List<String> wellKnownPeers;
    private List<String> blacklistedPeers;
    private String name;
    private String description;
    private String symbol;
    private String prefix;
    private String project;
    private String genesisLocation;
    private Map<Integer, BlockchainProperties> blockchainProperties;

    @JsonCreator
    public Chain(@JsonProperty("chainId") UUID chainId,
                 @JsonProperty("wellKnownPeers") List<String> wellKnownPeers,
                 @JsonProperty("name") String name,
                 @JsonProperty("description") String description,
                 @JsonProperty("symbol") String symbol,
                 @JsonProperty("prefix") String prefix,
                 @JsonProperty("project") String project,
                 @JsonProperty("genesisLocation") String genesisLocation,
                 @JsonProperty("blockchainProperties") List<BlockchainProperties> blockchainProperties
    ) {
        this(chainId, false, Collections.emptyList(), wellKnownPeers, Collections.emptyList(), name, description, symbol, prefix, project, genesisLocation,
                blockchainProperties);
    }
    public Chain(UUID chainId,
                 boolean active,
                 List<String> defaultPeers,
                 List<String> wellKnownPeers,
                 List<String> blacklistedPeers,
                 String name,
                 String description,
                 String symbol,
                 String prefix,
                 String project,
                 String genesisLocation,
                 List<BlockchainProperties> blockchainProperties
    ) {
        this.chainId = chainId;
        this.active = active;
        this.defaultPeers = defaultPeers;
        this.wellKnownPeers = wellKnownPeers;
        this.blacklistedPeers = blacklistedPeers;
        this.name = name;
        this.description = description;
        this.symbol = symbol;
        this.prefix = prefix;
        this.project = project;
        this.genesisLocation = genesisLocation;
        this.blockchainProperties =
                blockchainProperties
                        .stream()
                        .sorted(Comparator.comparingInt(BlockchainProperties::getHeight))
                        .collect(
                                Collectors.toMap(BlockchainProperties::getHeight, bp -> bp,
                                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }

    public Chain() {
    }

    public void setChainId(UUID chainId) {
        this.chainId = chainId;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setDefaultPeers(List<String> defaultPeers) {
        this.defaultPeers = defaultPeers;
    }

    public void setWellKnownPeers(List<String> wellKnownPeers) {
        this.wellKnownPeers = wellKnownPeers;
    }

    public void setBlacklistedPeers(List<String> blacklistedPeers) {
        this.blacklistedPeers = blacklistedPeers;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public void setGenesisLocation(String genesisLocation) {
        this.genesisLocation = genesisLocation;
    }

    public void setBlockchainProperties(Map<Integer, BlockchainProperties> blockchainProperties) {
        this.blockchainProperties = blockchainProperties;
    }

    public UUID getChainId() {
        return chainId;
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

    public boolean isActive() {
        return active;
    }

    public String getGenesisLocation() {
        return genesisLocation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Chain)) return false;
        Chain chain = (Chain) o;
        return active == chain.active &&
                Objects.equals(chainId, chain.chainId) &&
                Objects.equals(defaultPeers, chain.defaultPeers) &&
                Objects.equals(wellKnownPeers, chain.wellKnownPeers) &&
                Objects.equals(blacklistedPeers, chain.blacklistedPeers) &&
                Objects.equals(name, chain.name) &&
                Objects.equals(description, chain.description) &&
                Objects.equals(symbol, chain.symbol) &&
                Objects.equals(prefix, chain.prefix) &&
                Objects.equals(project, chain.project) &&
                Objects.equals(genesisLocation, chain.genesisLocation) &&
                Objects.equals(blockchainProperties, chain.blockchainProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chainId, active, defaultPeers, wellKnownPeers, blacklistedPeers, name, description, symbol, prefix, project, genesisLocation, blockchainProperties);
    }

    public Chain copy() {
        List<String> defaultPeersCopy = new ArrayList<>(defaultPeers);
        List<String> wellKnownPeersCopy = new ArrayList<>(wellKnownPeers);
        List<String> blacklistedPeersCopy = new ArrayList<>(blacklistedPeers);
        List<BlockchainProperties> blockchainPropertiesCopy = blockchainProperties.values().stream().map(BlockchainProperties::copy).collect(Collectors.toList());
        return new Chain(chainId, active, defaultPeersCopy, wellKnownPeersCopy, blacklistedPeersCopy, name, description, symbol, prefix, project,
                genesisLocation, blockchainPropertiesCopy);
    }

    @Override
    public String toString() {
        return "Chain{" +
                "chainId=" + chainId +
                ", active=" + active +
                ", defaultPeers=" + defaultPeers +
                ", wellKnownPeers=" + wellKnownPeers +
                ", blacklistedPeers=" + blacklistedPeers +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", symbol='" + symbol + '\'' +
                ", prefix='" + prefix + '\'' +
                ", project='" + project + '\'' +
                ", genesisLocation='" + genesisLocation + '\'' +
                ", blockchainProperties=" + blockchainProperties +
                '}';
    }

    @JsonGetter("blockchainProperties")
    public List<BlockchainProperties> getBlockchainPropertiesList() {
        return new ArrayList<>(blockchainProperties.values());
    }

    public Map<Integer, BlockchainProperties> getBlockchainProperties() {
        return blockchainProperties;
    }
}
