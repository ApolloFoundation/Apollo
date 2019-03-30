/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.chainid;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.db.BlockDao;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.util.env.config.BlockchainProperties;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * <p>To provide height-based config changing as described in conf/chains.json it used special listener that, depending
 *  on current height, change part of config represented by {@link HeightConfig}</p>
 *  <p>Note that this class is not thread-safe and cannot be used without additional synchronization. Its important especially, when dynamic
 *  chain switch will be implemented and {@link BlockchainConfigUpdater#updateChain} method will be called not only at startup but from different
 *  parts of
 *  application in concurrent environment</p>
 *  <p>Typically config should be updated to the latest height at application startup to provide correct config values for blockchain logic, such as
 * blockTime, adaptiveBlockTime, maxBalance and so on</p>
 */
@Singleton
public class BlockchainConfigUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(BlockchainConfigUpdater.class);

    private BlockDao blockDao;
    private BlockchainProcessor blockchainProcessor;
    private BlockchainConfig blockchainConfig;
    private Chain chain;
    
    @Inject
    public BlockchainConfigUpdater(BlockchainConfig blockchainConfig, BlockDao blockDao) {
        this.blockchainConfig = blockchainConfig;
        this.blockDao = blockDao;
    }

    public void updateChain(Chain chain) {
        this.chain = chain;
        blockchainConfig.updateChain(chain);
    }

    public void onBlockAccepted(@Observes @BlockEvent(BlockEventType.AFTER_BLOCK_ACCEPT) Block block) {
        tryUpdateConfig(block);
    }
    public void onBlockPopped(@Observes @BlockEvent(BlockEventType.BLOCK_POPPED) Block block) {
        tryUpdateConfig(block);
    }

    public void tryUpdateConfig(Block block) {
        int height = block.getHeight();
        BlockchainProperties bp = chain.getBlockchainProperties().get(height);
        if (bp != null) {
            updateToHeight(height, true);
        }
    }


    public void reset() {
        updateToHeight(0, true);
    }

    private BlockchainProcessor lookupBlockchainProcessor() {
        if (blockchainProcessor == null) {
            blockchainProcessor = CDI.current().select(BlockchainProcessor.class).get();
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

    void updateToHeight(int height, boolean inclusive) {

        HeightConfig latestConfig = getConfigAtHeight(height, inclusive);
        if (blockchainConfig.getCurrentConfig() != null) {
            LOG.debug("Update to {} at height {}", latestConfig, height);
            blockchainConfig.setCurrentConfig(latestConfig);
        } else {
            LOG.error("No configs at all! Proceed with old config {}", blockchainConfig.getCurrentConfig());
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
}
