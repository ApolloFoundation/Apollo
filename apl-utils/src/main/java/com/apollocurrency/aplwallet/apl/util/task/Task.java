/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.task;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * The object that represents a background task.
 */
@Getter
@Builder
@ToString
public class Task implements TaskAttributes, Runnable {

    /**
     * To be executed by a thread
     */
    @Setter
    private Runnable task;
    /**
     * Task name
     */
    private String name;
    /**
     * The initial delay (in milliseconds)
     */
    private int initialDelay;
    /**
     * The periodic delay (in milliseconds)
     */
    private int delay;

    @Override
    public void run() {
        getTask().run();
    }
}
