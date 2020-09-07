/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.observer;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.MemPool;
import com.apollocurrency.aplwallet.apl.core.service.state.ShufflerService;
import com.apollocurrency.aplwallet.apl.core.shard.DbHotSwapConfig;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;

@Slf4j
@Singleton
public class ShufflerObserver {
    private ShufflerService shufflerService;
    private MemPool memPool;

    @Inject
    public ShufflerObserver(ShufflerService shufflerService, MemPool memPool) {
        this.shufflerService = shufflerService;
        this.memPool = memPool;
    }

    public void onBlockApplied(@Observes @BlockEvent(BlockEventType.AFTER_BLOCK_APPLY) Block block) {
        log.trace(":accept:ShufflerObserver: START onBlockApply AFTER_BLOCK_APPLY, block={}", block.getHeight());
        Set<String> expired = shufflerService.getExpirations().get(block.getHeight());
        if (expired != null) {
            expired.forEach(e -> shufflerService.removeShufflingsByHash(e));
            shufflerService.removeExpirationsByHeight(block.getHeight());
            log.trace(":accept:ShufflerObserver:  onBlockApply AFTER_BLOCK_APPLY, block={}, expired=[{}]",
                block.getHeight(), expired.size());
        }
        log.trace(":accept:ShufflerObserver: END onBlockApplaid AFTER_BLOCK_APPLY, block={}", block.getHeight());
    }

    public void onBlockAccepted(@Observes @BlockEvent(BlockEventType.AFTER_BLOCK_ACCEPT) Block block) {
        log.debug(":accept:ShufflerObserver: START onAfterBlockAccept AFTER_BLOCK_ACCEPT, block height={}, shufflingsMap=[{}]",
            block.getHeight(), shufflerService.getShufflingsMap().size());

        shufflerService.getShufflingsMap().values().forEach(shufflerMap -> shufflerMap.values().forEach(shuffler -> {
            if (shuffler.getFailedTransaction() != null) {
                try {
                    memPool.softBroadcast(shuffler.getFailedTransaction());
                    shuffler.setFailedTransaction(null);
                    shuffler.setFailureCause(null);
                } catch (AplException.ValidationException ignore) {
                }
            }
        }));
    }

    public void onRescanBegan(@Observes @BlockEvent(BlockEventType.RESCAN_BEGIN) Block block) {
        shufflerService.stopAllShufflers();
    }

    public void onDbHotSwapBegin(@Observes DbHotSwapConfig config) {
        shufflerService.stopAllShufflers();
    }
}

