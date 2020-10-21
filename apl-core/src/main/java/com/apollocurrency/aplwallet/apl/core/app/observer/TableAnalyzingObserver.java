/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.observer;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.ObservesAsync;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

public class TableAnalyzingObserver {

    private static final Logger log = LoggerFactory.getLogger(TableAnalyzingObserver.class);

    private DatabaseManager databaseManager;

    private BlockchainProcessor blockchainProcessor;

    @Inject
    public TableAnalyzingObserver(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    //async
    public void onBlockPushed(@ObservesAsync @BlockEvent(BlockEventType.BLOCK_PUSHED) Block block) {
        if (block.getHeight() % 5000 == 0) {
            log.info("received block " + block.getHeight());
        }
    }

    //async
    public void onRescanEnd(@ObservesAsync @BlockEvent(BlockEventType.RESCAN_END) Block block) {
    }

    private BlockchainProcessor lookupBlockchainProcessor() {
        if (blockchainProcessor == null) {
            blockchainProcessor = CDI.current().select(BlockchainProcessor.class).get();
        }
        return blockchainProcessor;
    }
}
