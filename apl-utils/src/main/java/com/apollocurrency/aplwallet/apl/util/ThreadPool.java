/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ThreadPool {
    private static final Logger LOG = getLogger(ThreadPool.class);

    private static volatile ScheduledExecutorService scheduledThreadPool;
    private static Map<Runnable, Long> backgroundJobs = new HashMap<>();
    private static Map<Runnable, String> beforeStartJobs = new LinkedHashMap<>();
    private static Map<Runnable, String> lastBeforeStartJobs = new LinkedHashMap<>();
    private static Map<Runnable, String> afterStartJobs = new LinkedHashMap<>();

    public static synchronized void runBeforeStart(String name, Runnable runnable, boolean runLast) {
        if (scheduledThreadPool != null) {
            throw new IllegalStateException("Executor service already started");
        }
        if (runLast) {
            lastBeforeStartJobs.put(runnable, name);
        } else {
            beforeStartJobs.put(runnable, name);
        }
    }

    public static synchronized void runAfterStart(String name, Runnable runnable) {
        afterStartJobs.put(runnable, name);
    }

    public static synchronized void scheduleThread(String name, Runnable runnable, int delay) {
        scheduleThread(name, runnable, delay, TimeUnit.SECONDS);
    }

    public static synchronized void scheduleThread(String name, Runnable runnable, int delay, TimeUnit timeUnit) {
        if (scheduledThreadPool != null) {
            throw new IllegalStateException("Executor service already started, no new jobs accepted");
        }

//TODO: OL uncomment after cleanup of property reading in whole
/*
        if (aplGlobalObjects == null) {
            aplGlobalObjects = CDI.current().select(AplGlobalObjects.class).get();
        }
        if (! aplGlobalObjects.getBooleanProperty("apl.disable" + name + "Thread")) {
        */
            backgroundJobs.put(runnable, timeUnit.toMillis(delay));
        /*
        } else {
            LOG.info("Will not run " + name + " thread");
        }
*/
    }

    public static synchronized void start() {
        if (scheduledThreadPool != null) {
            throw new IllegalStateException("Executor service already started");
        }

        LOG.debug("Running " + beforeStartJobs.size() + " tasks...");
        runAll(beforeStartJobs);
        beforeStartJobs = null;

        LOG.debug("Running " + lastBeforeStartJobs.size() + " final tasks...");
        runAll(lastBeforeStartJobs);
        lastBeforeStartJobs = null;

        LOG.debug("Starting " + backgroundJobs.size() + " background jobs");
        scheduledThreadPool = Executors.newScheduledThreadPool(backgroundJobs.size(), new ThreadFactoryImpl("scheduled background pool"));
        for (Map.Entry<Runnable,Long> entry : backgroundJobs.entrySet()) {
            scheduledThreadPool.scheduleWithFixedDelay(entry.getKey(), 0, entry.getValue(), TimeUnit.MILLISECONDS);
        }
        backgroundJobs = null;

        LOG.debug("Starting " + afterStartJobs.size() + " delayed tasks");
        Thread thread = new Thread(() -> {
            runAll(afterStartJobs);
            afterStartJobs = null;
        });
        thread.setDaemon(true);
        thread.start();
    }

    public static void shutdown() {
        if (scheduledThreadPool != null) {
	        LOG.info("Stopping background jobs...");
            shutdownExecutor("ScheduledBackgroundPool", scheduledThreadPool, 10);
            scheduledThreadPool = null;
        	LOG.info("...Done");
        }
    }

    public static void shutdownExecutor(String name, ExecutorService executor, int timeout) {
        LOG.info("shutting down " + name);
        executor.shutdown();
        try {
            executor.awaitTermination(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (! executor.isTerminated()) {
            LOG.info("some threads in " + name + " didn't terminate, forcing shutdown");
            executor.shutdownNow();
        }
    }

    private static void runAll(Map<Runnable, String> jobs) {
        List<Thread> threads = new ArrayList<>();
        final StringBuffer errors = new StringBuffer();
        for (Map.Entry<Runnable, String> job: jobs.entrySet()) {
            Thread thread = new Thread(() -> {
                try {
                    job.getKey().run();
                } catch (Throwable t) {
                    errors.append(t.getMessage()).append('\n');
                    throw t;
                }
            }, job.getValue());
            thread.setDaemon(true);
            thread.start();
            threads.add(thread);
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (errors.length() > 0) {
            throw new RuntimeException("Errors running startup tasks:\n" + errors.toString());
        }
    }

    private ThreadPool() {} //never

}
