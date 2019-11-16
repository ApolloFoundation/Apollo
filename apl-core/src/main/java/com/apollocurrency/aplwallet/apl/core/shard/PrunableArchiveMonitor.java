/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.TrimService;
import com.apollocurrency.aplwallet.apl.core.task.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import com.apollocurrency.aplwallet.apl.util.task.TaskOrder;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import static com.apollocurrency.aplwallet.apl.util.Constants.DEFAULT_PRUNABLE_UPDATE_PERIOD;
import static com.apollocurrency.aplwallet.apl.util.Constants.PRUNABLE_MONITOR_DELAY;
import static com.apollocurrency.aplwallet.apl.util.Constants.PRUNABLE_MONITOR_INITIAL_DELAY;

@Slf4j
@Singleton
public class PrunableArchiveMonitor {

    private TimeService timeService;
    private ShardPrunableZipHashCalculator hashCalculator;
    private TaskDispatchManager taskManager;
    private TrimService trimService;
    private final ReentrantLock lock = new ReentrantLock();

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
        taskManager.newScheduledDispatcher("PrunableArchiveMonitor")
                .schedule(Task.builder()
                        .name("RecalculatePrunableZipHash")
                        .initialDelay(PRUNABLE_MONITOR_INITIAL_DELAY)
                        .task(this::processPrunableDataArchive)
                        .delay(PRUNABLE_MONITOR_DELAY)
                        .build(), TaskOrder.TASK);
        log.info("PrunableArchiveMonitor initialized, initial delay={} ms, delay={} ms.",
                PRUNABLE_MONITOR_INITIAL_DELAY,
                PRUNABLE_MONITOR_DELAY );
    }

    private void processPrunableDataArchive(){
        int epochTime = timeService.getEpochTime();
        int pruningTime = epochTime - epochTime % DEFAULT_PRUNABLE_UPDATE_PERIOD;
        log.debug("processPrunableDataArchive started pruningTime={} ", pruningTime);
        trimService.waitTrimming();
        lock.lock();
        try {

            hashCalculator.tryRecalculatePrunableArchiveHashes(pruningTime);
        }finally {
            lock.unlock();
        }
        log.debug("processPrunableDataArchive finished");
    }

    public boolean isProcessing() {
        return lock.isLocked();
    }

}
