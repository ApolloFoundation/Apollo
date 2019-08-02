/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.task;

import java.util.concurrent.RejectedExecutionException;

public interface TaskDispatcher extends TaskExecutorService, TaskDispatcherConfig{

    void dispatch();

    default boolean schedule(Task task) throws RejectedExecutionException {
        return schedule(task, TaskOrder.TASK);
    }

    boolean schedule(Task task, TaskOrder order) throws RejectedExecutionException;

    void shutdown();

    /**
     * Returns {@code true} if this executor has been shut down.
     *
     * @return {@code true} if this executor has been shut down
     */
    boolean isShutdown();

    String info();

}
