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

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * <p>This class used as configuration of current working chain. Commonly it mapped to an active chain described in conf/chains.json</p>
 * <p>To provide height-based config changing as described in conf/chains.json it used special listener that, depending
 * on current height, change part of config represented by {@link HeightConfig}</p>
 *
 * <p>Note that this class is thread-safe and can be used without additional synchronization after {@link BlockchainConfig#init} method call</p>
 * <p>Typically config should be updated to the latest height at application startup to provide correct config values for blockchain logic, such as
 * blockTime, adaptiveBlockTime, maxBalance and so on</p>
 */

@Singleton
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
    // lastKnownBlock must also be set in html/www/js/ars.constants.js
    private short shufflingProcessingDeadline;
    private long lastKnownBlock;
    private long unconfirmedPoolDepositAtm;
    private long shufflingDepositAtm;
    private int guaranteedBalanceConfirmations;
    private static BlockchainProcessor blockchainProcessor;
    private static BlockDao blockDao;

    private volatile HeightConfig currentConfig;
    private Chain chain;

    public BlockchainConfig() { //for weld
//        this(chainIdService,
//             holder.getIntProperty("apl.testnetLeasingDelay", -1),
//             holder.getIntProperty("apl.testnetGuaranteedBalanceConfirmations", -1),
//             holder.getIntProperty("apl.maxPrunableLifetime")
//                );
    }
    public void update(PropertiesHolder holder, Chain chain) {
                holder.getIntProperty("apl.maxPrunableLifetime");

    }
    public BlockchainConfig(int maxPrunableLifetime) {

        try {
            this.chain = chainIdService.getActiveChain();
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot get active chain");
        }
        this.testnet                        = chain.isTestnet();
        this.projectName                    = chain.getProject();
        this.accountPrefix                  = chain.getPrefix();
        this.coinSymbol                     = chain.getSymbol();
        this.leasingDelay                   = 1440;
        this.minPrunableLifetime            = 14 * 1440 * 60;
        this.shufflingProcessingDeadline    = 100;
        this.lastKnownBlock                 = 0;
        this.unconfirmedPoolDepositAtm      = 100 * Constants.ONE_APL;
        this.shufflingDepositAtm            = 1000 * Constants.ONE_APL;
        this.guaranteedBalanceConfirmations = 1440;
        this.enablePruning = maxPrunableLifetime >= 0;
        this.maxPrunableLifetime = enablePruning ? Math.max(maxPrunableLifetime, minPrunableLifetime) : Integer.MAX_VALUE;
    }

    public void init() {

        currentConfig = new HeightConfig(chain.getBlockchainProperties().get(0), testnet);
        ConfigChangeListener configChangeListener = new ConfigChangeListener(chain.getBlockchainProperties());
        lookupBlockchainProcessor().addListener(configChangeListener,
                BlockchainProcessor.Event.AFTER_BLOCK_ACCEPT);
        lookupBlockchainProcessor().addListener(configChangeListener,
                BlockchainProcessor.Event.BLOCK_POPPED);
        LOG.debug("Connected to chain {} - {}. ChainId - {}", chain.getName(), chain.getDescription(), chain.getChainId());
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
                currentConfig = new HeightConfig(propertiesMap.get(currentHeight), testnet);
                LOG.info("New config applied: {}", currentConfig);
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
