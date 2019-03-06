/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.sharding;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.ObservesAsync;

public class ShardingObserver {
    private static final Logger log = LoggerFactory.getLogger(ShardingObserver.class);

    private BlockchainProcessor blockchainProcessor;
    private BlockchainConfig blockchainConfig;
    private DatabaseManager databaseManager;

    public ShardingObserver(BlockchainProcessor blockchainProcessor, BlockchainConfig blockchainConfig, DatabaseManager databaseManager) {
        this.blockchainProcessor = blockchainProcessor;
        this.blockchainConfig = blockchainConfig;
        this.databaseManager = databaseManager;
    }

    public void onBlockPushed(@ObservesAsync @BlockEvent(BlockEventType.BLOCK_PUSHED) Block block) {
        HeightConfig currentConfig = blockchainConfig.getCurrentConfig();
        if (currentConfig.isShardingEnabled()) {
            if (blockchainProcessor.getMinRollbackHeight() % currentConfig.getShardingFrequency() == 0) {
                log.info("Start sharding....");
                databaseManager.getDataSource().begin();
//                List<Block> blocks = blockchainProcessor.popOffTo(blockchainProcessor.getMinRollbackHeight());

                databaseManager.getDataSource().commit();

            }
        }
    }
}
