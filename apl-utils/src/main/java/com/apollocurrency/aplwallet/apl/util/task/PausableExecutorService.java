/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.task;

import java.util.concurrent.ExecutorService;

/**
 * An {@link ExecutorService} that provides methods to manage the executor behavior
 * for suspending and resuming task execution.
 */
public interface PausableExecutorService extends ExecutorService {

    default boolean isRunning() {
        return !isPaused();
    }

    boolean isPaused();

    /**
     * Suspend the execution
     */
    void suspend();

    /**
     * Resume the execution
     */
    void resume();

}
