/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.observer;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.service.state.PollService;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author silaev-firstbridge on 6/11/2020
 */
@Singleton
@Slf4j
public class PollObserver {
    private final PollService pollService;

    @Inject
    public PollObserver(PollService pollService) {
        this.pollService = pollService;
    }

    public void onBlockApplied(@Observes @BlockEvent(BlockEventType.AFTER_BLOCK_APPLY) Block block) {
        final int height = block.getHeight();
        log.trace(":accept:PollObserver: START onBlockApplied AFTER_BLOCK_APPLY. height={}", height);
        pollService.checkPolls(height);
        log.trace(":accept:PollObserver: END onBlockApplied AFTER_BLOCK_APPLY. height={}", height);
    }
}
