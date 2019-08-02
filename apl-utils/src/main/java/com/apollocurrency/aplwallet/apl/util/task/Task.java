/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.task;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class Task implements TaskAttributes, Runnable{

    @Setter
    private Runnable task;
    private String name;
    private int initialDelay;
    private int delay;

    @Override
    public void run() {
        getTask().run();
    }
}
