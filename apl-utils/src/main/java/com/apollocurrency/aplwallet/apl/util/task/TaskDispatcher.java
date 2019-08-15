/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.task;

import java.util.concurrent.RejectedExecutionException;

/**
 * A dispatcher that provides methods to schedule the background {@link Task}.
 * There are four independent queues for background tasks - INIT, BEFORE, TASK and AFTER.
 * The TaskDispatcher executes all tasks in strong order.
 *
 */
public interface TaskDispatcher extends TaskExecutorService, TaskDispatcherConfig{

    /**
     * Create the main executor to run the background tasks accordingly their order.
     * The main executor runs all scheduled tasks.
     */
    void dispatch();

    /**
     * Submit a background task for execution as a background TASK.
     * @param task the task to execute
     * @return {@code true} if this task was put into a queue
     * @throws RejectedExecutionException if this dispatcher has been shut down
     */
    default boolean schedule(Task task) throws RejectedExecutionException {
        return schedule(task, TaskOrder.TASK);
    }

    /**
     * Submit a background task for execution. This task is put in queue and will be executed in specified order
     * @param task the task to execute
     * @param order the task order
     * @return {@code true} if the task is put into a queue
     * @throws RejectedExecutionException if this dispatcher has been shut down
     */
    boolean schedule(Task task, TaskOrder order) throws RejectedExecutionException;

    /**
     * Initiates an orderly shutdown in which previously submitted tasks are executed,
     * but no new tasks will be accepted. Invocation has no additional effect if already shut down.
     */
    void shutdown();

    /**
     * Returns {@code true} if this dispatcher has been shut down.
     */
    boolean isShutdown();

    /**
     * Return the representation of this dispatcher with any additional information  as a String
     */
    String info();

}
