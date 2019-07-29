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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TrimObserver {
    private static final Logger log = LoggerFactory.getLogger(TrimObserver.class);
    private TrimService trimService;
    private volatile boolean trimDerivedTables = true;
    private int trimFrequency;
    private final Object lock = new Object();
    private List<Integer> trimHeights = Collections.synchronizedList(new ArrayList<>());

    private boolean processTrimEvent() {
        if (trimDerivedTables) {
            synchronized (lock) {
                if (trimDerivedTables) {
                    if (!trimHeights.isEmpty()) {
                        List<Integer> targetHeights = trimHeights.stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList()); // start from minimal height
                        targetHeights.forEach(h-> {
                            log.debug("Perform trim on height " + h);
                            trimService.trimDerivedTables(h, true);
                        });
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public TrimObserver(TrimService trimService,
                         int trimFrequency) {
        this.trimService = trimService;
        this.trimFrequency = trimFrequency;
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
        if (block.getHeight() % 5000 == 0) {
            log.info("processed block " + block.getHeight());
        }
        if (trimDerivedTables && block.getHeight() % trimFrequency == 0) {
            trimService.doTrimDerivedTablesOnBlockchainHeight(block.getHeight(), false);
        }
    }

    // async
    public void onBlockPushed(@ObservesAsync @BlockEvent(BlockEventType.BLOCK_PUSHED) Block block) {
        if (block.getHeight() % trimFrequency == 0) {
            trimHeights.add(block.getHeight());
        }
        processTrimEvent();
    }
}
