/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.observer;


import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.Shuffling;
import com.apollocurrency.aplwallet.apl.core.service.state.ShufflingService;
import com.apollocurrency.aplwallet.apl.util.Constants;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class ShufflingObserver {

    private final BlockchainConfig blockchainConfig;
    private final ShufflingService shufflingService;

    @Inject
    public ShufflingObserver(BlockchainConfig blockchainConfig,
                             ShufflingService shufflingService) {
        this.blockchainConfig = blockchainConfig;
        this.shufflingService = shufflingService;
    }

    public void onBlockApplied(@Observes @BlockEvent(BlockEventType.AFTER_BLOCK_APPLY) Block block) {
        log.trace(":accept:ShufflingObserver: START onBlockApplaid AFTER_BLOCK_APPLY, block={}", block.getHeight());
        long startTime = System.currentTimeMillis();
        log.trace("Shuffling observer call at {}", block.getHeight());
        if (block.getOrLoadTransactions().size() == blockchainConfig.getCurrentConfig().getMaxNumberOfTransactions()
            || block.getPayloadLength() > blockchainConfig.getCurrentConfig().getMaxPayloadLength() - Constants.MIN_TRANSACTION_SIZE) {
            log.trace("Will not process shufflings at {}", block.getHeight());
            return;
        }
        List<Shuffling> shufflings = new ArrayList<>();
        List<Shuffling> activeShufflings = shufflingService.getActiveShufflings();//CollectionUtil.toList(getActiveShufflings(0, -1));
        log.trace("Got {} active shufflings at {} in {} ms", activeShufflings.size(), block.getHeight(), System.currentTimeMillis() - startTime);
        for (Shuffling shuffling : activeShufflings) {
            if (!shufflingService.isFull(shuffling, block)) {
                shufflings.add(shuffling);
            } else {
                log.trace("Skip shuffling {}, block is full", block.getId());
            }
        }
        log.trace("Shufflings to process - {} ", shufflings.size());
        int cancelled = 0, inserted = 0;
        for (Shuffling shuffling : shufflings) {
            shuffling.setBlocksRemaining((short) (shuffling.getBlocksRemaining() - 1));

            if (shuffling.getBlocksRemaining() <= 0) {
                cancelled++;
                shufflingService.cancel(shuffling, block);
            } else {
                log.trace("Insert shuffling {} - height - {} remaining - {}",
                    shuffling.getId(), shuffling.getHeight(), shuffling.getBlocksRemaining());
                inserted++;
                shufflingService.save(shuffling);
            }
        }
        log.trace(":accept: Shuffling observer, inserted [{}], cancelled [{}] in time: {} msec", inserted, cancelled, System.currentTimeMillis() - startTime);
    }


}
