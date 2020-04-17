/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.task;

/**
 * An factory class to construct {@link TaskExecutorService}
 */
public interface ExecutorServiceFactory {
    /**
     * Constructs a new {@code ExecutorService}.
     *
     * @param poolName a pool name
     * @param poolSize a pool size
     * @param daemon   true if new thread pool will contain daemons
     * @return constructed executor, or {@code null}
     */
    TaskExecutorService newExecutor(String poolName, int poolSize, boolean daemon);
}
