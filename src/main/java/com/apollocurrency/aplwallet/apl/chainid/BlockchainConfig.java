/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.chainid;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.apollocurrency.aplwallet.apl.AplGlobalObjects;
import com.apollocurrency.aplwallet.apl.Block;
import com.apollocurrency.aplwallet.apl.BlockImpl;
import com.apollocurrency.aplwallet.apl.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.Constants;
import com.apollocurrency.aplwallet.apl.PropertiesLoader;
import com.apollocurrency.aplwallet.apl.util.Listener;
import org.slf4j.Logger;

public class BlockchainConfig {
    private static final Logger LOG = getLogger(BlockchainConfig.class);

    private boolean testnet;
    private String projectName;
    private String accountPrefix;
    private String coinSymbol;
    private int leasingDelay;
    private int minPrunableLifetime;
    private boolean enablePruning;
    private int maxPrunableLifetime;
    private short shufflingProcessingDeadline;
    // lastKnownBlock must also be set in html/www/js/ars.constants.js
    private long lastKnownBlock;
    private long unconfirmedPoolDepositAtm;
    private long shufflingDepositAtm;
    private int guaranteedBalanceConfirmations;


    private volatile HeightConfig currentConfig;
    private Chain chain;

    public BlockchainConfig(Chain chain, PropertiesLoader loader) {
        this(chain,
             loader.getIntProperty("apl.testnetLeasingDelay", -1),
             loader.getIntProperty("apl.testnetGuaranteedBalanceConfirmations", -1),
             loader.getIntProperty("apl.maxPrunableLifetime")
                );
    }
    public BlockchainConfig(Chain chain, int testnetLeasingDelay, int testnetGuaranteedBalanceConfirmations, int maxPrunableLifetime) {

        this.chain                          = chain;
        this.testnet                        = chain.isTestnet();
        this.projectName                    = chain.getProject();
        this.accountPrefix                  = chain.getPrefix();
        this.coinSymbol                     = chain.getSymbol();
        this.leasingDelay                   = testnet ? testnetLeasingDelay == -1 ? 1440 : testnetLeasingDelay : 1440;
        this.minPrunableLifetime            = testnet ? 1440 * 60 : 14 * 1440 * 60;
        this.shufflingProcessingDeadline    = (short)(testnet ? 10 : 100);
        this.lastKnownBlock                 = testnet ? 0 : 0;
        this.unconfirmedPoolDepositAtm      = (testnet ? 50 : 100) * Constants.ONE_APL;
        this.shufflingDepositAtm            = (testnet ? 7 : 1000) * Constants.ONE_APL;
        this.guaranteedBalanceConfirmations = testnet ? testnetGuaranteedBalanceConfirmations == -1 ? 1440 : testnetGuaranteedBalanceConfirmations : 1440;
        this.enablePruning = maxPrunableLifetime >= 0;
        this.maxPrunableLifetime = enablePruning ? Math.max(maxPrunableLifetime, minPrunableLifetime) : Integer.MAX_VALUE;
    }
    public void init() {

        currentConfig = new HeightConfig(chain.getBlockchainProperties().get(0), testnet);
        ConfigChangeListener configChangeListener = new ConfigChangeListener(chain.getBlockchainProperties());
        BlockchainProcessorImpl.getInstance().addListener(configChangeListener,
                BlockchainProcessor.Event.AFTER_BLOCK_ACCEPT);
        BlockchainProcessorImpl.getInstance().addListener(configChangeListener,
                BlockchainProcessor.Event.BLOCK_POPPED);
        LOG.debug("Connected to chain {} - {}. ChainId - {}", chain.getName(), chain.getDescription(), chain.getChainId());
    }

    public void updateToLatestConfig() {
        BlockImpl lastBlock = AplGlobalObjects.getBlockDb().findLastBlock();
        if (lastBlock == null) {
            LOG.debug("Nothing to update. No blocks");
            return;
        }
        updateToHeight(lastBlock.getHeight(), true);
    }

    public void reset() {
        updateToHeight(0, true);
    }
    private void updateToHeight(int height, boolean inclusive) {
        Objects.requireNonNull(chain);

        HeightConfig latestConfig = getConfigAtHeight(height, inclusive);
        if (currentConfig != null) {
            LOG.info("Set current config to {}", currentConfig);
            currentConfig = latestConfig;
        } else {
            LOG.error("No configs at all!");
        }
    }

    public void rollback(int height) {
        LOG.info("Rollback blockchain config to {} height", height);
        updateToHeight(height, true);
    }

    private HeightConfig getConfigAtHeight(int targetHeight, boolean inclusive) {
        Map<Integer, BlockchainProperties> blockchainProperties = chain.getBlockchainProperties();
        if (targetHeight == 0) {
            return new HeightConfig(blockchainProperties.get(0), testnet);
        }
        Optional<Integer> maxHeight =
                blockchainProperties
                        .keySet()
                        .stream()
                        .filter(height -> inclusive ? targetHeight >= height : targetHeight > height)
                        .max(Comparator.naturalOrder());
        return maxHeight
                .map(height -> new HeightConfig(blockchainProperties.get(height), testnet))
                .orElse(null);
    }



    private class ConfigChangeListener implements Listener<Block> {
        private final Map<Integer, BlockchainProperties> propertiesMap;
        private final Set<Integer> targetHeights;

        public ConfigChangeListener(Map<Integer, BlockchainProperties> propertiesMap) {
            this.propertiesMap = new ConcurrentHashMap<>(propertiesMap);
            this.targetHeights = Collections.unmodifiableSet(propertiesMap.keySet());
            String stringConfigChangeHeights =
                    targetHeights.stream().map(Object::toString).collect(Collectors.joining(
                            ","));
            LOG.debug("Config updates at heights: {}",
                    stringConfigChangeHeights.isEmpty() ? "none" : stringConfigChangeHeights);
        }

        @Override
        public void notify(Block block) {
            int currentHeight = block.getHeight();
            if (targetHeights.contains(currentHeight)) {
                LOG.info("Updating chain config at height {}", currentHeight);
                currentConfig = new HeightConfig(propertiesMap.get(currentHeight), testnet);
                LOG.info("New chain config applied: {}", currentConfig);
            }
        }
    }

    public boolean isTestnet() {
        return testnet;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getAccountPrefix() {
        return accountPrefix;
    }

    public String getCoinSymbol() {
        return coinSymbol;
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
}
