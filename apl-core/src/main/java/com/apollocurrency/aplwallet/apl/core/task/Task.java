/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.task;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class Task implements TaskAttributes, Runnable{

    private Runnable task;
    private String name;
    private boolean daemon;
    private boolean recurring;
    private int initialDelay;
    private int delay;

    @Override
    public void run() {
        getTask().run();
    }
}
