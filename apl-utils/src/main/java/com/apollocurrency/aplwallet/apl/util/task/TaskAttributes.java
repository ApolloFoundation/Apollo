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
     * @return the class represents the background thread
     */
    Runnable getTask();

    /**
     *
     * @return true if the thread is a daemon thread
     */
    boolean isDaemon();

    /**
     *
     * @return true if task should be repeated
     */
    boolean isRecurring();


    /**
     *
     * @return the thread name
     */
    String getName();

    /**
     *
     * @return the threads group name
     */
    String getGroup();


}
