/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.observer;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.TrimService;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimConfigUpdated;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
public class TrimObserver {
    private static final Logger log = LoggerFactory.getLogger(TrimObserver.class);
    private final TrimService trimService;
    private volatile boolean trimDerivedTables = true;
    private int trimFrequency;
    private final Object lock = new Object();
    private final Queue<Integer> trimHeights = new PriorityQueue<>(); // will sort heights from lowest to highest automatically
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    void init() {
        executorService.scheduleWithFixedDelay(this::processTrimEvent, 0, 2, TimeUnit.SECONDS);
    }

    @PreDestroy
    void shutdown() {
        executorService.shutdownNow();
    }

    boolean isTrimDerivedTables() {
        return trimDerivedTables;
    }

    private void processTrimEvent() {
        if (trimDerivedTables) {
            Integer trimHeight = null;
            synchronized (lock) {
                if (trimDerivedTables) {
                    trimHeight = trimHeights.poll();
                }
            }
            if (trimHeight != null) {
                log.debug("Perform trim on blockchain height {}", trimHeight);
                trimService.trimDerivedTables(trimHeight, true);
            }
        }
    }

    @Inject
    public TrimObserver(TrimService trimService) {
        this.trimService = trimService;
        this.trimFrequency = Constants.DEFAULT_TRIM_FREQUENCY;
    }


    public void onTrimConfigUpdated(@Observes @TrimConfigUpdated Boolean trimDerivedTables) {
        this.trimDerivedTables = trimDerivedTables;
    }

    public void onBlockScanned(@Observes @BlockEvent(BlockEventType.BLOCK_SCANNED) Block block) {
        //TODO: please replace 5000 with meaningfull constant name
        if (block.getHeight() % 5000 == 0) {
            log.info("Scan: processed block " + block.getHeight());
        }
        if (trimDerivedTables && block.getHeight() % trimFrequency == 0) {
            trimService.doTrimDerivedTablesOnBlockchainHeight(block.getHeight(), false);
        }
    }

    public void onBlockAccepted(@Observes @BlockEvent(BlockEventType.AFTER_BLOCK_ACCEPT) Block block) {
        if (block.getHeight() % trimFrequency == 0) {
            synchronized (lock) {
                trimHeights.add(block.getHeight());
            }
        }
    }
}
