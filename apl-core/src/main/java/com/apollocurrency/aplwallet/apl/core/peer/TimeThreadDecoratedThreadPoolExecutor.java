/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TimeThreadDecoratedThreadPoolExecutor extends DecoratedThreadPoolExecutor {

    public TimeThreadDecoratedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public TimeThreadDecoratedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public TimeThreadDecoratedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public TimeThreadDecoratedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    public Event event() {
        return new TimeThreadJob();
    }


    @Slf4j
    private static class TimeThreadJob implements Event {
        private static final AtomicInteger taskIdCounter = new AtomicInteger(0);
        private volatile long startTime;
        private volatile long scheduleTime;
        private volatile long finishTime;
        private volatile long taskId;

        @Override
        public void before() {
            startTime = System.currentTimeMillis();
        }

        @Override
        public void after() {
            finishTime = System.currentTimeMillis();
            log.debug("Async task #{} finished, fullTime/taskTime {}/{}", taskId, finishTime - scheduleTime, finishTime - startTime);
        }

        @Override
        public void atStart() {
            this.taskId = taskIdCounter.incrementAndGet();
            log.debug("Async task #{} started, trace {}", taskId, ThreadUtils.lastNStacktrace(15));
            scheduleTime = System.currentTimeMillis();
        }
    }
}
