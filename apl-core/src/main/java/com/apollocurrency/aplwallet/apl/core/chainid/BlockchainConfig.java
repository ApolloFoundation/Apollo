/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.chainid;

import com.apollocurrency.aplwallet.apl.util.env.config.BlockchainProperties;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * <p>This class used as configuration of current working chain. Commonly it mapped to an active chain described in conf/chains.json</p>
 */

@Slf4j
@Singleton
public class BlockchainConfig {
    static final int DEFAULT_MIN_PRUNABLE_LIFETIME = 14 * 1440 * 60; // two weeks in seconds

    private int leasingDelay;
    private int minPrunableLifetime;
    private boolean enablePruning;
    private int maxPrunableLifetime;
    private short shufflingProcessingDeadline;
    private long lastKnownBlock;
    private long unconfirmedPoolDepositAtm;
    private long shufflingDepositAtm;
    private int guaranteedBalanceConfirmations;
    private volatile HeightConfig currentConfig;
    private volatile Optional<HeightConfig> previousConfig = Optional.empty(); // keep a previous config for easy access
    private Chain chain;
    private TreeMap<Integer, HeightConfig> heightConfigMap = new TreeMap<>();
    private volatile boolean isJustUpdated = false;

    public BlockchainConfig() {
    }

    public BlockchainConfig(Chain chain, PropertiesHolder holder) {
        updateChain(chain, holder);
    }

    public void updateChain(Chain chain, int minPrunableLifetime, int maxPrunableLifetime) {

        Objects.requireNonNull(chain, "Chain cannot be null");
        setFields(chain, minPrunableLifetime, maxPrunableLifetime);
        Map<Integer, BlockchainProperties> blockchainProperties = chain.getBlockchainProperties();
        if (blockchainProperties.isEmpty() || blockchainProperties.get(0) == null) {
            throw new IllegalArgumentException("Chain has no initial blockchain properties at height 0! ChainId = " + chain.getChainId());
        }
        heightConfigMap = blockchainProperties.values()
            .stream()
            .map((BlockchainProperties bp) -> new HeightConfig(bp, getOneAPL(), getInitialSupply()))
            .sorted(Comparator.comparing(HeightConfig::getHeight))
            .collect(Collectors.toMap(HeightConfig::getHeight, Function.identity(), (old, newv)-> newv, TreeMap::new));
        currentConfig = heightConfigMap.get(0);
        log.debug("Switch to chain {} - {}. ChainId - {}", chain.getName(), chain.getDescription(), chain.getChainId());
    }

    public HeightConfig getConfigAtHeight(int targetHeight) {
        HeightConfig heightConfig = heightConfigMap.get(targetHeight);
        if (heightConfig != null) {
            return heightConfig;
        }
        Optional<Integer> maxHeight =
            heightConfigMap
                .keySet()
                .stream()
                .filter(height -> targetHeight >= height)
                .max(Comparator.naturalOrder());
        return maxHeight
            .map(height -> heightConfigMap.get(height))
            .orElse(null);
    }

    /**
     * Return previous configuration for any given height. Rollback is considered too.
     * @param targetHeight specified height value used for selecting 'previous' config
     * @return optional configuration
     */
    public Optional<HeightConfig> getPreviousConfigByHeight(int targetHeight) {
        log.trace("getPreviousConfigByHeight targetHeight = '{}'", targetHeight);
        Optional<HeightConfig> result = Optional.empty();
        Set<Map.Entry<Integer, HeightConfig>> entries = heightConfigMap.headMap(targetHeight, true).entrySet();
        log.trace("getPreviousConfigByHeight entries: '{}'\n", entries);
        for (Map.Entry<Integer, HeightConfig> configEntry : entries) {
            Map.Entry<Integer, HeightConfig> prev = heightConfigMap.lowerEntry(configEntry.getKey());  // previous
            Map.Entry<Integer, HeightConfig> next = heightConfigMap.higherEntry(configEntry.getKey()); // next
            log.trace("prev = {}", prev);
            log.trace("current = {}", configEntry);
            log.trace("next = {}\n", next);
            if (prev != null && previousConfig(targetHeight).test(prev.getKey())) {
                result = Optional.ofNullable(prev.getValue());
            }
        }
        log.trace("getPreviousConfigByHeight, targetHeight = '{}' RESULT = {}\n", targetHeight, result);
        return result;
    }

    public static Predicate<Integer> previousConfig(Integer height) {
        return targetHeight -> targetHeight < height;
    }

    private void setFields(Chain chain, int minPrunableLifetime, int maxPrunableLifetime) {
        this.chain = chain;
        // These fields could be static constants but some of them should be scaled by blockTime
        // Block time scaling should be implemented in future
        this.leasingDelay = 1440;
        this.minPrunableLifetime = minPrunableLifetime > 0 ? minPrunableLifetime : DEFAULT_MIN_PRUNABLE_LIFETIME;
        this.shufflingProcessingDeadline = (short) 100;
        this.lastKnownBlock = 0;
        this.unconfirmedPoolDepositAtm = Math.multiplyExact(100, chain.getOneAPL());
        this.shufflingDepositAtm = Math.multiplyExact(1000, chain.getOneAPL());
        this.guaranteedBalanceConfirmations = 1440;
        this.enablePruning = maxPrunableLifetime >= 0;
        this.maxPrunableLifetime = enablePruning ? Math.max(maxPrunableLifetime, this.minPrunableLifetime) : Integer.MAX_VALUE;
    }

    void updateChain(Chain chain, PropertiesHolder holder) {
        int maxPrunableLifetime = holder.getIntProperty("apl.maxPrunableLifetime");
        int minPrunableLifetime = holder.getIntProperty("apl.minPrunableLifetime");
        updateChain(chain, minPrunableLifetime, maxPrunableLifetime);
    }

    void updateChain(Chain chain) {
        updateChain(chain, 0, 0);
    }

    public String getProjectName() {
        return chain.getProject();
    }

    public long getInitialSupply() {
        return chain.getInitialSupply();
    }

    public int getDecimals() {
        return chain.getDecimals();
    }

    public long getOneAPL() {
        return chain.getOneAPL();
    }

    public String getAccountPrefix() {
        return chain.getPrefix();
    }

    public String getCoinSymbol() {
        return chain.getSymbol();
    }

    public int getLeasingDelay() {
        return leasingDelay;
    }

    public int getMinPrunableLifetime() {
        return minPrunableLifetime;
    }

    public short getShufflingProcessingDeadline() {
        return shufflingProcessingDeadline;
    }

    public long getLastKnownBlock() {
        return lastKnownBlock;
    }

    public long getUnconfirmedPoolDepositAtm() {
        return unconfirmedPoolDepositAtm;
    }

    public long getShufflingDepositAtm() {
        return shufflingDepositAtm;
    }

    public int getGuaranteedBalanceConfirmations() {
        return guaranteedBalanceConfirmations;
    }

    public boolean isEnablePruning() {
        return enablePruning;
    }

    public int getMaxPrunableLifetime() {
        return maxPrunableLifetime;
    }

    public Integer getDexPendingOrdersReopeningHeight() {
        if (chain.getFeaturesHeightRequirement() != null) {
            return chain.getFeaturesHeightRequirement().getDexReopenPendingOrdersHeight();
        } else {
            return null;
        }
    }

    public Integer getDexExpiredContractWithFinishedPhasingHeightAndStep3() {
        if (chain.getFeaturesHeightRequirement() != null) {
            return chain.getFeaturesHeightRequirement().getDexExpiredContractWithFinishedPhasingHeightAndStep3();
        } else {
            return null;
        }
    }

    public Optional<Integer> getTransactionV2Height() {
        if (chain.getFeaturesHeightRequirement() != null) {
            return Optional.ofNullable(chain.getFeaturesHeightRequirement().getTransactionV2Height());
        } else {
            return Optional.empty();
        }
    }

    public boolean isTransactionV2ActiveAtHeight(int height) {
        if (getTransactionV2Height().isPresent()) {
            return height >= getTransactionV2Height().get();
        }
        return false;
    }

    public HeightConfig getCurrentConfig() {
        return currentConfig;
    }

    /**
     * For UNIT TEST only!
     *
     * @param currentConfig configuration to be assigned as current
     */
    public void setCurrentConfig(HeightConfig currentConfig) {
        this.previousConfig = Optional.ofNullable(this.currentConfig);
        this.currentConfig = currentConfig;
        this.isJustUpdated = true; // setup flag to catch chains.json config change on APPLY_BLOCK
    }

    public Chain getChain() {
        return chain;
    }

    public Optional<HeightConfig> getPreviousConfig() {
        return previousConfig;
    }

    public void setPreviousConfig(Optional<HeightConfig> previousConfig) {
        this.previousConfig = previousConfig;
    }

    /**
     * Flag to catch configuration changing
     * // TODO: YL after separating 'shard' and 'trim' logic, we can remove 'isJustUpdated() + resetJustUpdated()' usage
     *
     * @return if config was recently updated to new height
     */
    public boolean isJustUpdated() {
        return isJustUpdated;
    }

    public void resetJustUpdated() {
        this.isJustUpdated = false; // reset flag
    }

    /**
     * @param fromHeight height from which height configs should be fetched (inclusive)
     * @param toHeight height to which height configs should be fetched (exclusive)
     * @return ordered list of height configs (from lover height to higher) between given heights
     */
    public List<HeightConfig> getAllActiveConfigsBetweenHeights(int fromHeight, int toHeight) {
        if (fromHeight >= toHeight) {
            throw new IllegalArgumentException("fromHeight should be lesser than toHeight, given: fromHeight=" + fromHeight + ", toHeight=" + toHeight);
        }
        HeightConfig configAtHeight = getConfigAtHeight(fromHeight - 1); // active config at the beginning of [fromHeight; toHeight] range

        ArrayList<HeightConfig> heightConfigs = new ArrayList<>(heightConfigMap
            .entrySet()
            .stream()
            .filter(e -> e.getKey() >= fromHeight && e.getKey() < toHeight)
            .map(Map.Entry::getValue)
            .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(HeightConfig::getHeight)))));
        if (configAtHeight != null) {
            heightConfigs.add(configAtHeight);
        }
        return heightConfigs;

    }
}
