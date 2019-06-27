/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.observer;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.TrimService;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimConfigUpdated;
import com.apollocurrency.aplwallet.apl.core.config.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Singleton
public class TrimObserver {
    private static final Logger log = LoggerFactory.getLogger(TrimObserver.class);
    private TrimService trimService;
    private boolean trimDerivedTables;
    private int trimFrequency;
    private final List<Integer> trimHeights = new ArrayList<>();

    public void scheduleTrim(int height) {
        synchronized (trimHeights) {
            trimHeights.add(height);
        }
    }

    private void processTrimEvent() {
        synchronized (trimHeights) {
            if (!trimHeights.isEmpty()) {
                Integer height = trimHeights.remove(0);
                log.debug("Perform trim on height " + height);
                trimService.trimDerivedTables(height);
            }
        }
    }

    @Inject
    public TrimObserver(TrimService trimService,
                        @Property("apl.trimDerivedTables") boolean trimDerivedTables,
                        @Property("apl.trimFrequency") int trimFrequency) {
        this.trimService = trimService;
        this.trimDerivedTables = trimDerivedTables;
        this.trimFrequency = trimFrequency;
    }


    public void onTrimConfigUpdated(@Observes @TrimConfigUpdated Boolean trimDerivedTables) {
        this.trimDerivedTables = trimDerivedTables;
    }

    public void onBlockScanned(@Observes @BlockEvent(BlockEventType.BLOCK_SCANNED) Block block) {
        if (block.getHeight() % 5000 == 0) {
            log.info("processed block " + block.getHeight());
        }
        if (trimDerivedTables && block.getHeight() % trimFrequency == 0) {
            trimService.doTrimDerivedTablesOnBlockchainHeight(block.getHeight());
        }
    }

    // async
    public void onBlockPushed(@ObservesAsync @BlockEvent(BlockEventType.BLOCK_PUSHED) Block block) {
        if (trimDerivedTables && block.getHeight() % trimFrequency == 0) {
            scheduleTrim(block.getHeight());
        }
        processTrimEvent();
    }
}
