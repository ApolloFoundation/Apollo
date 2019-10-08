/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.task.limiter;

import com.apollocurrency.aplwallet.apl.util.task.NamedThreadFactory;
import com.apollocurrency.aplwallet.apl.util.task.Tasks;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.environment.util.Collections;

import javax.inject.Singleton;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Singleton
public class TimeLimiterServiceImpl implements TimeLimiterService {

    private static final String  PREFIX = "Limiter";
    public static final String MESSAGE_THE_SERVICE_IS_SHUT_DOWN = "The service is shut down.";
    private ConcurrentHashMap<String, LimiterEntry> allocatedLimiters;
    private volatile boolean started;

    public TimeLimiterServiceImpl() {
        allocatedLimiters = new ConcurrentHashMap<>();
        started = true;
    }

    @Override
    public TimeLimiter acquireLimiter(String name) {
        Preconditions.checkState(started, MESSAGE_THE_SERVICE_IS_SHUT_DOWN);
        LimiterEntry limiter = allocatedLimiters.get(name);
        if(limiter == null){
            limiter = createLimiterEntry(name);
            allocatedLimiters.put(name, limiter);
        }
        return limiter.limiter;
    }

    @Override
    public void shutdown() {
        if (started) {
            started = false;
            Enumeration<String> keys = allocatedLimiters.keys();
            Collections.asList(keys).forEach(name -> Tasks.shutdownExecutor(name, allocatedLimiters.get(name).executor, 5));
        }else{
            log.warn("The service is already shut down.");
        }
    }

    /**
     * Return Executor service for the specified name
     * @param name  the name
     * @return the executor service if limiter is allocated or null;
     */
    public ExecutorService getExecutor(String name){
        if(!started && log.isDebugEnabled()){
            log.debug(MESSAGE_THE_SERVICE_IS_SHUT_DOWN);
        }
        LimiterEntry entry = allocatedLimiters.get(name);
        if (entry != null){
            return entry.executor;
        }
        return null;
    }

    private LimiterEntry createLimiterEntry(String name){
        ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory(String.format("%s-%s", PREFIX, name), true));
        LimiterEntry limiterEntry = new LimiterEntry(SimpleTimeLimiter.create(executor), executor);
        log.trace("Create new limiter {} with cachedThreadPool", name);
        return limiterEntry;
    }

    private static class LimiterEntry {
        TimeLimiter limiter;
        ExecutorService executor;

        LimiterEntry(TimeLimiter limiter, ExecutorService executor) {
            this.limiter = limiter;
            this.executor = executor;
        }
    }
}
