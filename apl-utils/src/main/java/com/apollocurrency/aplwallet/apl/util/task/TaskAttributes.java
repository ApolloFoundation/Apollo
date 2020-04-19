/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.task;

/**
 * The attributes for background tasks
 */
public interface TaskAttributes {

    /**
     * Returns the background thread
     *
     * @return the class represents the background thread
     */
    Runnable getTask();

    /**
     * @return the task name
     */
    String getName();

    /**
     * @return the initial delay in milliseconds
     */
    default int getInitialDelay() {
        return 0;
    }

    /**
     * @return the delay between starts in milliseconds
     */
    default int getDelay() {
        return 10;
    }

}
