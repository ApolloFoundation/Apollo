/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.task;

import java.util.concurrent.ExecutorService;

/**
 * An {@link ExecutorService} that provides methods to execute the background tasks.
 * TaskExecutorService can additionally suspend or resume the general thread executor
 */
public interface TaskExecutorService {

    /**
     * Returns <code>true</code> if specified task is valid for execution.
     *
     * @param task the task to validate
     * @return <code>true</code> if task is valid for execution
     */
    boolean validate(Task task);

    /**
     * Submit a task for execution
     *
     * @param task the task to submit
     */
    void invoke(Task task);

    /**
     * Suspend current executor {@link ExecutorService}
     */
    void suspend();

    /**
     * Resume current executor {@link ExecutorService}
     */
    void resume();

    /**
     * The main executor that executes submitted background tasks.
     *
     * @return the task executor
     */
    ExecutorService executor();
}
