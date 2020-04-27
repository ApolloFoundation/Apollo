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
     * Map for storing all ENABLED 'shard setting' instances available for current 'chain' config
     * The unmodified map stored 'height' and 'sharding setting' corresponding it.
     * Present shard settings are enabled ONLY !
     */
    private Map<Integer, ShardingSettings> enabledShardingSettingsMap;

    @Inject
    public BlockchainConfigUpdater(BlockchainConfig blockchainConfig, BlockDao blockDao) {
        this.blockchainConfig = blockchainConfig;
        this.blockDao = blockDao;
    }

    public void updateChain(Chain chain, PropertiesHolder propertiesHolder) {
        this.chain = chain;
        if (chain != null) {
            enabledShardingSettingsMap = null;// reset map for initialization again with new data
        }
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

    public void reset() {
        updateToHeight(0);
        if (chain != null) {
            enabledShardingSettingsMap = null;// reset map for initialization again with new data
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

    private HeightConfig getConfigAtHeight(int targetHeight) {
        if (this.chain == null) {
            String error = "Chain configuration is not initialized ! That's strange actually...";
            log.error(error);
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
     * Return correct ShardingSetting by specified (trim) height. We should find only ENABLED shard settings with height
     * lower when value passed into method.
     *
     * @param trimHeight target height (trim height usually)
     * @return found ShardingSettings value OR Optional.Empty value if not found
     */
    public Optional<ShardingSettings> getShardingSettingsByTrimHeight(int trimHeight) {
        if (this.chain == null) {
            String error = "Can't get 'ShardingSettings', because Chain configuration is not initialized ! That's strange actually...";
            log.warn(error);
            return Optional.empty();
        }
        Map<Integer, BlockchainProperties> blockchainProperties = chain.getBlockchainProperties();
        if (blockchainProperties == null) { // very small chance, but who knows...?
            String error = String.format("Missing any 'BlockchainProperties' by trimHeight '%s' !", trimHeight);
            log.warn(error);
            return Optional.empty();
        }
        if (trimHeight < 0) { // no chance to look for negative height value in configs
            String error = String.format("'trimHeight' is negative trimHeight '%s' !", trimHeight);
            log.warn(error);
            return Optional.empty();
        }
        if (enabledShardingSettingsMap == null) {
            // lazy initialization
            enabledShardingSettingsMap =
                blockchainProperties
                    .values().stream()
//                    .entrySet().stream()
//                    .sorted(Map.Entry.comparingByKey(new BlockchainProperties()).reversed())
//                    .filter(
//                        blockchainProperty -> blockchainProperty.getShardingSettings() != null
//                        && blockchainProperty.getShardingSettings().isEnabled()
//                    )
//                    .collect(Collectors.toMap(
//                        BlockchainProperties::getHeight,
//                        BlockchainProperties::getShardingSettings));

            .collect(Collectors.toMap(BlockchainProperties::getHeight, BlockchainProperties::getShardingSettings,
                (oldValue, newValue) -> oldValue, LinkedHashMap::new));
        }
        System.out.println("enabledShardingSettingsMap = " + enabledShardingSettingsMap.toString());
        return enabledShardingSettingsMap
            .entrySet().stream()
            // reverse order for the higher 'height' values in beginning
//            .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
            .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
//            .filter(entry -> entry.getKey() <= trimHeight)
//            .filter(entry -> entry.getKey() < trimHeight)
//            .filter(entry -> entry.getKey() >= trimHeight)
//            .map(entry -> new ShardingSettings( entry.getKey(), entry.getValue()))
//            .collect(Collectors.toList());
//            .findFirst();
//            .reduce((sh1, sh2 ) -> sh1.getStartHeight() >= trimHeight && trimHeight < sh2.getStartHeight() ? sh1 : sh2 );
            .reduce((sh1, sh2 ) -> {
//                System.out.println("trimHeight = " + trimHeight + " sh1(" + sh1.getStartHeight() + ") VS sh2(" + sh2.getStartHeight() + ")");
                System.out.println("trimHeight = " + trimHeight + " sh1(" + sh1.getKey() + ") VS sh2(" + sh2.getKey() + ")");
//                if (sh1.getStartHeight() <= trimHeight && trimHeight < sh2.getStartHeight()) {
                if (sh1.getKey() <= trimHeight && trimHeight < sh2.getKey()) {
//                    System.out.println("return sh1 = " + sh1.getStartHeight());
                    System.out.println("return sh1 = " + sh1.getKey());
                    return sh1;
                } else {
//                    System.out.println("return sh2 = " + sh2.getStartHeight());
                    System.out.println("return sh2 = " + sh2.getKey());
                    return sh2;
                }
            }).map(entry -> new ShardingSettings( entry.getKey(), entry.getValue()))
            ;
    }

    /**
     * Used by unit tests mostly.
     * @return Return UNMODIFIED map
     */
    public Map<Integer, ShardingSettings> getEnabledShardingSettingsMap() {
        return enabledShardingSettingsMap;
    }
}
