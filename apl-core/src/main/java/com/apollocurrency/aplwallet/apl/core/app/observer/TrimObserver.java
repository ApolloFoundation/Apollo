/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.observer;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.TrimService;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.config.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TrimObserver {
    private static final Logger log = LoggerFactory.getLogger(TrimObserver.class);

    private TrimService trimService;
    private volatile boolean isTrimming;
    private boolean trimDerivedTables;
    private int trimFrequency;

    @Inject
    public TrimObserver(TrimService trimService,
                        @Property("apl.trimDerivedTables") boolean trimDerivedTables,
                        @Property("apl.trimFrequency") int trimFrequency) {
        this.trimService = trimService;
        this.trimDerivedTables = trimDerivedTables;
        this.trimFrequency = trimFrequency;
    }

    public void onBlockScanned(@Observes @BlockEvent(BlockEventType.BLOCK_SCANNED) Block block) {
        if (block.getHeight() % 5000 == 0) {
            log.info("processed block " + block.getHeight());
        }
        if (trimDerivedTables && block.getHeight() % trimFrequency == 0) {
            trimService.doTrimDerivedTables(block.getHeight(), null);
        }
    }

    // async
    public void onBlockPushed(@ObservesAsync @BlockEvent(BlockEventType.BLOCK_PUSHED) Block block) {
        if (trimDerivedTables && block.getHeight() % trimFrequency == 0 && !isTrimming) {
            isTrimming = true;
            trimService.trimDerivedTables(block.getHeight());
            isTrimming = false;
        }
    }
}
