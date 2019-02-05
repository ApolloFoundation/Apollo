/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.chainid;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockDao;
import com.apollocurrency.aplwallet.apl.core.app.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.Constants;
import com.apollocurrency.aplwallet.apl.util.Listener;
import com.apollocurrency.aplwallet.apl.util.env.config.BlockchainProperties;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;

/**
 * <p>This class used as configuration of current working chain. Commonly it mapped to an active chain described in conf/chains.json</p>
 * <p>To provide height-based config changing as described in conf/chains.json it used special listener that, depending
 * on current height, change part of config represented by {@link HeightConfig}</p>
 *
 * <p>Note that this class is not thread-safe and cannot be used without additional synchronization. Its important especially, when dynamic
 * chain switch will be implemented and {@link BlockchainConfig#updateChain} method will be called not only at startup but from different parts of
 * application in concurrent environment</p>
 * <p>Typically config should be updated to the latest height at application startup to provide correct config values for blockchain logic, such as
 * blockTime, adaptiveBlockTime, maxBalance and so on</p>
 */

@Singleton
public class BlockchainConfig {
    private static final Logger LOG = getLogger(BlockchainConfig.class);
    private static BlockchainProcessor blockchainProcessor;
    private static BlockDao blockDao;
    private int leasingDelay;
    private int minPrunableLifetime;
    private boolean enablePruning;
    private int maxPrunableLifetime;
    // lastKnownBlock must also be set in html/www/js/ars.constants.js
    private short shufflingProcessingDeadline;
    private long lastKnownBlock;
    private long unconfirmedPoolDepositAtm;
    private long shufflingDepositAtm;
    private int guaranteedBalanceConfirmations;
    private volatile HeightConfig currentConfig;
    private ConfigChangeListener configChangeListener;
    private Chain chain;

    public BlockchainConfig() {}

    public void updateChain(Chain chain, int maxPrunableLifetime) {
        Objects.requireNonNull(chain, "Chain cannot be null");
        setFields(chain, maxPrunableLifetime);
        Map<Integer, BlockchainProperties> blockchainProperties = chain.getBlockchainProperties();
        if (blockchainProperties.size() == 0 || blockchainProperties.get(0) == null) {
            throw new IllegalArgumentException("Chain has no initial blockchain properties at height 0! ChainId = " + chain.getChainId());
        }
        currentConfig = new HeightConfig(blockchainProperties.get(0));
        deregisterConfigChangeListener();
        registerConfigChangeListener();
        LOG.debug("Switch to chain {} - {}. ChainId - {}", chain.getName(), chain.getDescription(), chain.getChainId());
    }

    private void setFields(Chain chain, int maxPrunableLifetime) {
        this.chain = chain;
        // These fields could be static constants but some fields should be scaled by blockTime
        // Block time scaling should be implemented in future
        this.leasingDelay = 1440;
        this.minPrunableLifetime = 14 * 1440 * 60; // two weeks in seconds
        this.shufflingProcessingDeadline = (short) 100;
        this.lastKnownBlock = 0;
        this.unconfirmedPoolDepositAtm = 100 * Constants.ONE_APL;
        this.shufflingDepositAtm = 1000 * Constants.ONE_APL;
        this.guaranteedBalanceConfirmations = 1440;

        this.enablePruning = maxPrunableLifetime >= 0;
        this.maxPrunableLifetime = enablePruning ? Math.max(maxPrunableLifetime, minPrunableLifetime) : Integer.MAX_VALUE;
    }

    public BlockchainConfig(Chain chain, PropertiesHolder holder) {
        updateChain(chain, holder);
    }

    public void updateChain(Chain chain, PropertiesHolder holder) {
        int maxPrunableLifetime = holder.getIntProperty("apl.maxPrunableLifetime");
        updateChain(chain, maxPrunableLifetime);
    }

    public void updateChain(Chain chain) {
        updateChain(chain, 0);
    }

    public void registerConfigChangeListener() {
        configChangeListener = new ConfigChangeListener(chain.getBlockchainProperties());
        lookupBlockchainProcessor().addListener(configChangeListener,
                BlockchainProcessor.Event.AFTER_BLOCK_ACCEPT);
        lookupBlockchainProcessor().addListener(configChangeListener,
                BlockchainProcessor.Event.BLOCK_POPPED);
    }

    public void deregisterConfigChangeListener() {
        lookupBlockchainProcessor().removeListener(configChangeListener,
                BlockchainProcessor.Event.AFTER_BLOCK_ACCEPT);
        lookupBlockchainProcessor().removeListener(configChangeListener,
                BlockchainProcessor.Event.BLOCK_POPPED);
    }

    public void reset() {
        updateToHeight(0, true);
    }

    private BlockchainProcessor lookupBlockchainProcessor() {
        if (blockchainProcessor == null) {
            blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
        }
        return blockchainProcessor;
    }

    private BlockDao lookupBlockDao() {
        if (blockDao == null) {
            blockDao = CDI.current().select(BlockDaoImpl.class).get();
        }
        return blockDao;
    }

    public void updateToLatestConfig() {
        Block lastBlock = lookupBlockDao().findLastBlock();
        if (lastBlock == null) {
            LOG.debug("Nothing to update. No blocks");
            return;
        }
        updateToHeight(lastBlock.getHeight(), true);
    }

    private void updateToHeight(int height, boolean inclusive) {
        Objects.requireNonNull(chain);

        HeightConfig latestConfig = getConfigAtHeight(height, inclusive);
        if (currentConfig != null) {
            currentConfig = latestConfig;
        } else {
            LOG.error("No configs at all!");
        }
    }

    public void rollback(int height) {
        updateToHeight(height, true);
    }

    private HeightConfig getConfigAtHeight(int targetHeight, boolean inclusive) {
        Map<Integer, BlockchainProperties> blockchainProperties = chain.getBlockchainProperties();
        if (targetHeight == 0) {
            return new HeightConfig(blockchainProperties.get(0));
        }
        Optional<Integer> maxHeight =
                blockchainProperties
                        .keySet()
                        .stream()
                        .filter(height -> inclusive ? targetHeight >= height : targetHeight > height)
                        .max(Comparator.naturalOrder());
        return maxHeight
                .map(height -> new HeightConfig(blockchainProperties.get(height)))
                .orElse(null);
    }

    public String getProjectName() {
        return chain.getProject();
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

    public HeightConfig getCurrentConfig() {
        return currentConfig;
    }

    public Chain getChain() {
        return chain;
    }

    private class ConfigChangeListener implements Listener<Block> {
        private final Map<Integer, BlockchainProperties> propertiesMap;
        private final Set<Integer> targetHeights;

        public ConfigChangeListener(Map<Integer, BlockchainProperties> propertiesMap) {
            this.propertiesMap = new ConcurrentHashMap<>(propertiesMap);
            this.targetHeights = Collections.unmodifiableSet(propertiesMap.keySet());
            String stringConstantsChangeHeights =
                    targetHeights.stream().map(Object::toString).collect(Collectors.joining(
                            ","));
            LOG.debug("Constants updates at heights: {}",
                    stringConstantsChangeHeights.isEmpty() ? "none" : stringConstantsChangeHeights);
        }

        @Override
        public void notify(Block block) {
            int currentHeight = block.getHeight();
            if (targetHeights.contains(currentHeight)) {
                LOG.info("Updating chain config at height {}", currentHeight);
                currentConfig = new HeightConfig(propertiesMap.get(currentHeight));
                LOG.info("New config applied: {}", currentConfig);
            }
        }
    }
}
