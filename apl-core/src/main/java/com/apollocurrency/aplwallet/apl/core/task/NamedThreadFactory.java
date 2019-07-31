package com.apollocurrency.aplwallet.apl.core.task;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;
    private final boolean daemon;

    public NamedThreadFactory(String poolName) {
        this(poolName, false);
    }

    public NamedThreadFactory(String poolName, boolean daemon) {
        this(null, poolName, daemon);
    }

    public NamedThreadFactory(ThreadGroup group, String poolName, boolean daemon) {
        if (group == null){
            SecurityManager s = System.getSecurityManager();
            this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        }else{
            this.group = group;
        }
        this.namePrefix = poolName +"-";
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r,
                namePrefix + threadNumber.getAndIncrement(),
                0);
        try {
            if (t.isDaemon()) {
                if (!daemon) {
                    t.setDaemon(false);
                }
            } else {
                if (daemon) {
                    t.setDaemon(true);
                }
            }

            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);

        } catch (Exception ignored) {
            // Doesn't matter even if failed to set.
        }

        return t;
    }
}
