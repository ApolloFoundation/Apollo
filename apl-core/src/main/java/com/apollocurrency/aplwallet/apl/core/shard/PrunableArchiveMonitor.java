/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockchainEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockchainEventType;
import com.apollocurrency.aplwallet.apl.core.app.runnable.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TrimService;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcher;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;

import static com.apollocurrency.aplwallet.apl.util.Constants.DEFAULT_PRUNABLE_UPDATE_PERIOD;
import static com.apollocurrency.aplwallet.apl.util.Constants.PRUNABLE_MONITOR_DELAY;
import static com.apollocurrency.aplwallet.apl.util.Constants.PRUNABLE_MONITOR_INITIAL_DELAY;

@Slf4j
@Singleton
public class PrunableArchiveMonitor {

    private final TimeService timeService;
    private final ShardPrunableZipHashCalculator hashCalculator;
    private final TaskDispatchManager taskManager;
    private final TrimService trimService;
    private TaskDispatcher taskDispatcher;
    private volatile boolean processing = false;

    @Inject
    public PrunableArchiveMonitor(ShardPrunableZipHashCalculator hashCalculator, TaskDispatchManager taskManager,
                                  TimeService timeService, TrimService trimService) {
        this.hashCalculator = Objects.requireNonNull(hashCalculator);
        this.taskManager = Objects.requireNonNull(taskManager);
        this.timeService = Objects.requireNonNull(timeService);
        this.trimService = Objects.requireNonNull(trimService);
    }

    @PostConstruct
    public void init() {
        taskDispatcher = taskManager.newScheduledDispatcher("PrunableArchiveMonitor");
        taskDispatcher.schedule(Task.builder()
            .name("RecalculatePrunableZipHash")
            .initialDelay(PRUNABLE_MONITOR_INITIAL_DELAY)
            .task(this::processPrunableDataArchive)
            .delay(PRUNABLE_MONITOR_DELAY)
            .build());
        log.info("PrunableArchiveMonitor initialized, initial delay={} ms, delay={} ms.",
            PRUNABLE_MONITOR_INITIAL_DELAY,
            PRUNABLE_MONITOR_DELAY);
    }

    public void onResumeBlockchainEvent(@Observes @BlockchainEvent(BlockchainEventType.RESUME_DOWNLOADING) BlockchainConfig cfg) {
        resumeMonitor();
    }

    public void onSuspendBlockchainEvent(@Observes @BlockchainEvent(BlockchainEventType.SUSPEND_DOWNLOADING) BlockchainConfig cfg) {
        suspendMonitor();
    }

    public void resumeMonitor() {
        taskDispatcher.resume();
    }

    public void suspendMonitor() {
        taskDispatcher.suspend();
    }

    private void processPrunableDataArchive() {
        int epochTime = timeService.getEpochTime();
        int pruningTime = epochTime - epochTime % DEFAULT_PRUNABLE_UPDATE_PERIOD;
        log.debug("processPrunableDataArchive started pruningTime={} ", pruningTime);
        trimService.waitTrimming();
        processing = true;
        try {
            hashCalculator.tryRecalculatePrunableArchiveHashes(pruningTime);
        } finally {
            processing = false;
        }
        log.debug("processPrunableDataArchive finished");
    }

    public boolean isProcessing() {
        return processing;
    }

}
