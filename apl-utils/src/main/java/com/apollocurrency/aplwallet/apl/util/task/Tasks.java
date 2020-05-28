/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.task;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class consists exclusively of static methods that operate on {@link Task} or {@link ExecutorService}.
 */
@Slf4j
public class Tasks {

    /**
     * Initiates an orderly shutdown and blocks until all tasks have completed execution after a shutdown
     * request, or the timeout occurs.
     *
     * @param name     the executor name for logging
     * @param executor executor
     * @param timeout  the maximum time to wait in SECONDS
     */
    public static void shutdownExecutor(String name, ExecutorService executor, int timeout) {
        if (executor != null) {
            try {
                log.info("Shutting down {}", name);
                executor.shutdown();
                try {
                    executor.awaitTermination(timeout, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (!executor.isTerminated()) {
                    log.info("Some threads in {} didn't terminate, forcing shutdown", name);
                    executor.shutdownNow();
                }
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
            }
        }
    }
}
