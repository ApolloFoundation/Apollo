/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.chainid;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.db.BlockDao;
import com.apollocurrency.aplwallet.apl.util.env.config.BlockchainProperties;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

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
@Singleton
public class BlockchainConfigUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(BlockchainConfigUpdater.class);

    private BlockDao blockDao;
    private BlockchainConfig blockchainConfig;
    private Chain chain;

    @Inject
    public BlockchainConfigUpdater(BlockchainConfig blockchainConfig, BlockDao blockDao) {
        this.blockchainConfig = blockchainConfig;
        this.blockDao = blockDao;
    }

    public void updateChain(Chain chain, PropertiesHolder propertiesHolder) {
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

    public void reset() {
        updateToHeight(0);
    }


    public void updateToLatestConfig() {
        Block lastBlock = blockDao.findLastBlock();
        if (lastBlock == null) {
            LOG.debug("Nothing to update. No blocks");
            return;
        }
        updateToHeight(lastBlock.getHeight());
    }

    void updateToHeight(int height) {

        HeightConfig latestConfig = getConfigAtHeight(height);
        HeightConfig currentConfig = blockchainConfig.getCurrentConfig();
        if (currentConfig != null && latestConfig != null && currentConfig.getHeight() != latestConfig.getHeight()) {
            LOG.debug("Update to {} at height {}", latestConfig, height);
            blockchainConfig.setCurrentConfig(latestConfig);
        }
    }

    public void rollback(int height) {
        updateToHeight(height);
    }

    private HeightConfig getConfigAtHeight(int targetHeight) {
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
}
