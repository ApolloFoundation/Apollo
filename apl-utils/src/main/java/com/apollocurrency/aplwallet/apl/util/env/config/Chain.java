/*
 * Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@EqualsAndHashCode
@ToString
@JsonPropertyOrder({"chainId", "active", "defaultPeers", "wellKnownPeers", "blacklistedPeers", "name", "description", "symbol",
    "prefix", "project", "initialSupply", "decimals", "featuresHeightRequirement","currencyIssuanceHeights", "blockchainProperties"})
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
    private long initialSupply;
    private int decimals;
    private long oneAPL;
    private FeaturesHeightRequirement featuresHeightRequirement;
    private Set<Integer> currencyIssuanceHeights;
    private Set<String> totalAmountOverflowTxs;
    private Map<Integer, BlockchainProperties> blockchainProperties;

    @JsonCreator
    public Chain(@JsonProperty("chainId") UUID chainId,
                 @JsonProperty("wellKnownPeers") List<String> wellKnownPeers,
                 @JsonProperty("name") String name,
                 @JsonProperty("description") String description,
                 @JsonProperty("symbol") String symbol,
                 @JsonProperty("prefix") String prefix,
                 @JsonProperty("project") String project,
                 @JsonProperty("initialSupply") long initialSupply,
                 @JsonProperty("decimals") int decimals,
                 @JsonProperty("blockchainProperties") List<BlockchainProperties> blockchainProperties
    ) {
        this(chainId, false, Collections.emptyList(), wellKnownPeers, Collections.emptyList(),
            name, description, symbol, prefix, project, initialSupply, decimals,
             blockchainProperties, null, null, null);
    }

    /**
     * All fields Constructor.
     *
     * @param chainId id of the chain
     * @param active whether active this chain or not (typically true)
     * @param defaultPeers set of the peers to connect
     * @param wellKnownPeers set of the privileged peers to connect
     * @param blacklistedPeers set of the peers, which should be avoided
     * @param name name of the chain
     * @param description description of the chain
     * @param symbol ticker name of the coin on the chain
     * @param prefix account address prefix on the chain
     * @param project name of project for this chain
     * @param initialSupply             the initial supply in APL
     * @param decimals                  the decimals value to convert APL to ATM, 1APL = 10^decimals ATM
     * @param blockchainProperties height-based config of the network-wide consensus parameters
     * @param featuresHeightRequirement config to enable different features by height
     * @param currencyIssuanceHeights heights of the currency issuance transactions,
     *                                which were correct by the old broken currency re-issuance validation logic
     * @param totalAmountOverflowTxs unsigned ids of the currency sell transactions, which should not be validated early by total order
     *                        amount overflow of {@link Long} java type limits until new validation rules accepted
     */
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
                 long initialSupply,
                 int decimals,
                 List<BlockchainProperties> blockchainProperties,
                 FeaturesHeightRequirement featuresHeightRequirement,
                 Set<Integer> currencyIssuanceHeights,
                 Set<String> totalAmountOverflowTxs
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
        this.initialSupply = initialSupply;
        this.setDecimals(decimals);
        this.blockchainProperties =
            blockchainProperties
                .stream()
                .sorted(Comparator.comparingInt(BlockchainProperties::getHeight))
                .collect(
                    Collectors.toMap(BlockchainProperties::getHeight, bp -> bp,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
        this.featuresHeightRequirement = featuresHeightRequirement;
        this.currencyIssuanceHeights = currencyIssuanceHeights;
        this.totalAmountOverflowTxs = totalAmountOverflowTxs;
    }

    public Chain() {
    }

    public Set<String> getTotalAmountOverflowTxs() {
        return totalAmountOverflowTxs;
    }

    public void setTotalAmountOverflowTxs(Set<String> currencySellTxs) {
        this.totalAmountOverflowTxs = currencySellTxs;
    }

    public FeaturesHeightRequirement getFeaturesHeightRequirement() {
        return featuresHeightRequirement;
    }

    public void setFeaturesHeightRequirement(FeaturesHeightRequirement featuresHeightRequirement) {
        this.featuresHeightRequirement = featuresHeightRequirement;
    }

    public UUID getChainId() {
        return chainId;
    }

    public void setChainId(UUID chainId) {
        this.chainId = chainId;
    }

    public List<String> getDefaultPeers() {
        return defaultPeers;
    }

    public void setDefaultPeers(List<String> defaultPeers) {
        this.defaultPeers = defaultPeers;
    }

    public List<String> getWellKnownPeers() {
        return wellKnownPeers;
    }

    public void setWellKnownPeers(List<String> wellKnownPeers) {
        this.wellKnownPeers = wellKnownPeers;
    }

    public List<String> getBlacklistedPeers() {
        return blacklistedPeers;
    }

    public void setBlacklistedPeers(List<String> blacklistedPeers) {
        this.blacklistedPeers = blacklistedPeers;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public long getInitialSupply() {
        return initialSupply;
    }

    public void setInitialSupply(long initialSupply) {
        this.initialSupply = initialSupply;
    }

    public int getDecimals() {
        return decimals;
    }

    public void setDecimals(int decimals) {
        this.decimals = decimals;
        this.oneAPL = BigInteger.TEN.pow(decimals).longValue();
    }

    public long getOneAPL() {
        return oneAPL;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Chain copy() {
        List<String> defaultPeersCopy = new ArrayList<>(defaultPeers);
        List<String> wellKnownPeersCopy = new ArrayList<>(wellKnownPeers);
        List<String> blacklistedPeersCopy = new ArrayList<>(blacklistedPeers);
        List<BlockchainProperties> blockchainPropertiesCopy = blockchainProperties.values().stream().map(BlockchainProperties::copy).collect(Collectors.toList());
        return new Chain(chainId, active, defaultPeersCopy, wellKnownPeersCopy, blacklistedPeersCopy,
            name, description, symbol, prefix, project, initialSupply, decimals,
            blockchainPropertiesCopy, featuresHeightRequirement != null ? featuresHeightRequirement.copy() : null,
            currencyIssuanceHeights == null ? null : new HashSet<>(currencyIssuanceHeights),
            totalAmountOverflowTxs == null ? null : new HashSet<>(totalAmountOverflowTxs));
    }

    @JsonGetter("blockchainProperties")
    public List<BlockchainProperties> getBlockchainPropertiesList() {
        return new ArrayList<>(blockchainProperties.values());
    }

    public Map<Integer, BlockchainProperties> getBlockchainProperties() {
        return blockchainProperties;
    }

    public void setBlockchainProperties(Map<Integer, BlockchainProperties> blockchainProperties) {
        this.blockchainProperties = blockchainProperties;
    }

    public Set<Integer> getCurrencyIssuanceHeights() {
        return currencyIssuanceHeights;
    }

    public void setCurrencyIssuanceHeights(Collection<Integer> currencyIssuanceHeights) {
        if (currencyIssuanceHeights == null) {
            return;
        }
        this.currencyIssuanceHeights = new HashSet<>(currencyIssuanceHeights);
    }
}
