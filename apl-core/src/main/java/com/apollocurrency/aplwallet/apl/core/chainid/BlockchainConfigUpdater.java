/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.chainid;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.db.BlockDao;
import com.apollocurrency.aplwallet.apl.util.env.config.BlockchainProperties;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.config.ShardingSettings;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>To provide height-based config changing as described in conf/chains.json it used special listener that, depending
 * on current height, change part of config represented by {@link HeightConfig}</p>
 * <p>Note that this class is not thread-safe and cannot be used without additional synchronization. Its important especially, when dynamic
 * chain switch will be implemented and {@link BlockchainConfigUpdater#updateChain} method will be called not only at startup but from different
 * parts of
 * application in concurrent environment</p>
 * <p>Typically config should be updated to the latest height at application startup to provide correct config values for blockchain logic, such as
 * blockTime, adaptiveBlockTime, maxBalance and so on</p>
 */
@Slf4j
@Singleton
public class BlockchainConfigUpdater {

    private BlockDao blockDao;
    private BlockchainConfig blockchainConfig;
    private Chain chain;
    /**
     * Map for storing all 'shard setting' instances available for current 'chain' config
     * The map stores config 'height' as key and 'sharding setting' corresponding it as value.
     */
    private Map<Integer, ShardingSettings> shardingSettingsMap;

    @Inject
    public BlockchainConfigUpdater(BlockchainConfig blockchainConfig, BlockDao blockDao) {
        this.blockchainConfig = blockchainConfig;
        this.blockDao = blockDao;
    }

    public void updateChain(Chain chain, PropertiesHolder propertiesHolder) {
        if (this.chain != null && !this.chain.equals(chain)) {
            shardingSettingsMap = null;// reset map for initialization again by new data
        }
        this.chain = chain;
        blockchainConfig.updateChain(chain, propertiesHolder);
    }

    public void onBlockAccepted(@Observes @BlockEvent(BlockEventType.AFTER_BLOCK_ACCEPT) Block block) {
        BlockchainProperties bp = chain.getBlockchainProperties().get(block.getHeight());
        if (bp != null) {
            blockchainConfig.setCurrentConfig(new HeightConfig(bp));
        }
    }

    public void onBlockPopped(@Observes @BlockEvent(BlockEventType.BLOCK_POPPED) Block block) {
        updateToHeight(block.getHeight() - 1);
    }

    public synchronized void reset() {
        updateToHeight(0);
        if (chain != null) {
            shardingSettingsMap = null;// reset map for initialization again with new data
        }
    }

    public void updateToLatestConfig() {
        Block lastBlock = blockDao.findLastBlock();
        if (lastBlock == null) {
            log.debug("Nothing to update. No blocks");
            return;
        }
        updateToHeight(lastBlock.getHeight());
    }

    void updateToHeight(int height) {

        HeightConfig latestConfig = getConfigAtHeight(height);
        HeightConfig currentConfig = blockchainConfig.getCurrentConfig();
        if (currentConfig != null && latestConfig != null && currentConfig.getHeight() != latestConfig.getHeight()) {
            log.debug("Update to {} at height {}", latestConfig, height);
            blockchainConfig.setCurrentConfig(latestConfig);
        }
    }

    public void rollback(int height) {
        updateToHeight(height);
    }

    public HeightConfig getConfigAtHeight(int targetHeight) {
        if (this.chain == null) {
            String error = "Chain configuration is not initialized ! That's strange actually...";
            throw new RuntimeException(error);
        }
        Map<Integer, BlockchainProperties> blockchainProperties = chain.getBlockchainProperties();
        BlockchainProperties bpAtHeight = blockchainProperties.get(targetHeight);
        if (bpAtHeight != null) {
            return new HeightConfig(bpAtHeight);
        }
        Optional<Integer> maxHeight =
            blockchainProperties
                .keySet()
                .stream()
                .filter(height -> targetHeight >= height)
                .max(Comparator.naturalOrder());
        return maxHeight
            .map(height -> new HeightConfig(blockchainProperties.get(height)))
            .orElse(null);
    }

    public HeightConfig getCurrentConfig() {
        return blockchainConfig.getCurrentConfig();
    }

    /**
     * Return correct ShardingSetting by specified (trim) height. We find any shard settings with height
     * associated with height.
     *
     * @param trimHeight target height (trim height usually)
     * @return found ShardingSettings value OR Optional.Empty value if not found
     */
    public synchronized Optional<ShardingSettings> getShardingSettingsByTrimHeight(int trimHeight) {
        if (this.chain == null) {
            String error = "Can't get 'ShardingSettings', because Chain configuration is not initialized ! That's strange actually...";
            log.warn(error);
            return Optional.empty();
        }
        Map<Integer, BlockchainProperties> blockchainProperties = chain.getBlockchainProperties();
        if (blockchainProperties == null) { // very small chance, but who knows...?
            String error = String.format("Missing any 'BlockchainProperties' by trimHeight '%s' !", trimHeight);
            throw new RuntimeException(error);
        }
        if (trimHeight < 0) { // no chance to look for negative height value in configs
            String error = String.format("'trimHeight' is negative trimHeight '%s' !", trimHeight);
            throw new RuntimeException(error);
        }
        if (shardingSettingsMap == null) {
            // lazy initialization and caching data inside LinkedMap for correct ordering
            shardingSettingsMap =
                blockchainProperties
                    .values().stream()
            .collect(Collectors.toMap(BlockchainProperties::getHeight, BlockchainProperties::getShardingSettings,
                (oldValue, newValue) -> oldValue, LinkedHashMap::new));
        }
        if (log.isTraceEnabled()) {
            log.trace("enabledShardingSettingsMap = " + shardingSettingsMap.toString());
        }
        Optional<Integer> maxHeight =
            blockchainProperties
                .keySet()
                .stream()
                .filter(height -> trimHeight >= height)
                .max(Comparator.naturalOrder());
        return maxHeight.map(height -> shardingSettingsMap.get(height));
    }

    /**
     * Used by unit tests mostly.
     * @return Return map
     */
    public Map<Integer, ShardingSettings> getShardingSettingsMap() {
        return shardingSettingsMap;
    }
}
