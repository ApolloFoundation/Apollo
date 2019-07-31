package com.apollocurrency.aplwallet.apl.core.task;

import java.util.concurrent.ExecutorService;

public interface ExecutorServiceFactory {
    /**
     * Constructs a new {@code ExecutorService}.
     *
     * @param poolName a pool name
     * @param poolSize a pool size
     * @param daemon true if new thread pool will contain daemons
     *
     * @return constructed executor, or {@code null}
     */
    ExecutorService newExecutor(String poolName, int poolSize, boolean daemon);
}
