/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.chainid;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.BlockDao;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
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
@Slf4j
@Singleton
public class BlockchainConfigUpdater {

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
        HeightConfig config = blockchainConfig.getConfigAtHeight(block.getHeight());
        if (config != null) {
            blockchainConfig.setCurrentConfig(config);
        }
    }

    public void onBlockPopped(@Observes @BlockEvent(BlockEventType.BLOCK_POPPED) Block block) {
        updateToHeight(block.getHeight() - 1);
    }

    public synchronized void reset() {
        updateToHeight(0);
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
        HeightConfig latestConfig = blockchainConfig.getConfigAtHeight(height);
        HeightConfig currentConfig = blockchainConfig.getCurrentConfig();
        if (currentConfig != null && latestConfig != null && currentConfig.getHeight() != latestConfig.getHeight()) {
            log.debug("Update to config '{}' at height {}", latestConfig.getHeight(), height);
            blockchainConfig.setCurrentConfig(latestConfig);
        }
        if (latestConfig != null) { // update previous config
            Optional<HeightConfig> previousConfig = blockchainConfig.getPreviousConfigByHeight(latestConfig.getHeight());
            blockchainConfig.setPreviousConfig(previousConfig);
        }
    }

    public void rollback(int height) {
        updateToHeight(height);
    }

}
