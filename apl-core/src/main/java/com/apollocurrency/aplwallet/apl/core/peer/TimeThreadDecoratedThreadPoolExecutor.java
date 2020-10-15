/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

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
        private volatile long startTime;
        private volatile long scheduleTime;
        private volatile long finishTime;

        @Override
        public void before() {
            startTime = System.currentTimeMillis();
        }

        @Override
        public void after() {
            finishTime = System.currentTimeMillis();
            log.debug("Async task finished, fullTime/taskTime {}/{}", finishTime - scheduleTime, finishTime - startTime);
        }

        @Override
        public void atStart() {
            scheduleTime = System.currentTimeMillis();
        }
    }
}
