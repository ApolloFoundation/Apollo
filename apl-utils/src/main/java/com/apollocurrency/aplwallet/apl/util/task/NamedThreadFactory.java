package com.apollocurrency.aplwallet.apl.util.task;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An {@link ThreadFactory} that creates new threads used by an Executor in the
 * same {@link ThreadGroup}.
 * <p>Each new thread is created as a daemon thread (according to the constructor
 * parameter) with priority set to the {@code Thread.NORM_PRIORITY}.
 * <p>New threads have names accessible via {@link Thread#getName} of
 * <em>poolName-N</em>, where <em>poolName</em> is the prefix, this prefix set
 * as a parameter in constructor and <em>N</em> is the sequence number of the thread
 * created by this factory.
 */
@Slf4j
public class NamedThreadFactory implements ThreadFactory {
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;
    private final boolean daemon;

    /**
     * Create a new ThreadFactory with the given pool name
     */
    public NamedThreadFactory(String poolName) {
        this(poolName, false);
    }

    /**
     * Create a new ThreadFactory with the given initial parameters
     */
    public NamedThreadFactory(String poolName, boolean daemon) {
        this(null, poolName, daemon);
    }

    /**
     * Create a new ThreadFactory with the given initial parameters
     */
    public NamedThreadFactory(ThreadGroup group, String poolName, boolean daemon) {
        if (group == null) {
            SecurityManager s = System.getSecurityManager();
            this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        } else {
            this.group = group;
        }
        this.namePrefix = poolName + "-";
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
            t.setUncaughtExceptionHandler((thread, e) -> log.error("Thread " + thread.getName() + " thrown an exception",e));

        } catch (Exception ignored) {
        }

        return t;
    }
}
