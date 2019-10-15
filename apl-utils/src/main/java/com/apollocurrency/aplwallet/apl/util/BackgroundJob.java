/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;


public class BackgroundJob {
    private final String name;
    private final long delay;
    private final Runnable job;

    public String getName() {
        return name;
    }

    public long getDelay() {
        return delay;
    }

    public Runnable getJob() {
        return job;
    }

    public BackgroundJob(String name, long delay, Runnable job) {
        this.name = name;
        this.delay = delay;
        this.job = job;
    }
}
