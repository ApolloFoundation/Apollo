/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.task;

import java.util.concurrent.ExecutorService;

public interface PausableExecutorService extends ExecutorService {

    default boolean isRunning() {
        return !isPaused();
    }

    boolean isPaused();

    /**
     * Pause the execution
     */
    void suspend();

    /**
     * Resume pool execution
     */
    void resume();

}
