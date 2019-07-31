/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.task;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.util.collections.Multimap;
import org.jboss.weld.util.collections.ListMultimap;

import java.util.*;
import java.util.concurrent.*;

@Slf4j
public abstract class AbstractTaskDispatcher implements TaskDispatcher {

    public static final int DEFAULT_THREAD_POOL_SIZE = 10;
    public static final String APL_BKG_WORKERS = "apl-bkg-workers";
    public static final String APL_POOL_NAME = "apl-dispatcher";

    protected ExecutorServiceFactory executorServiceFactory;
    protected ExecutorService backgroundThread;
    @Getter
    protected String serviceName;
    @Getter
    protected boolean disabled;
    protected volatile boolean started = false;

    private ExecutorService onStartExecutor;
    private Multimap<TaskOrder, Task> tasks = new ListMultimap<>();
    private Object taskMonitor = new Object();

    public AbstractTaskDispatcher(ExecutorServiceFactory executorServiceFactory, String name) {
        this(executorServiceFactory, name, false);
    }

    public AbstractTaskDispatcher(ExecutorServiceFactory executorServiceFactory, String name, boolean disabled) {
        this.executorServiceFactory = Objects.requireNonNull(executorServiceFactory, "ExecutorFactory is NULL");
        this.serviceName = Objects.requireNonNull(name, "Service name is NULL");
        this.disabled = disabled;
        if (disabled) {
            log.warn("Thread service {} is disabled in configuration.", serviceName);
        } else {
            this.onStartExecutor = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE,
                    new NamedThreadFactory(new ThreadGroup(APL_BKG_WORKERS), APL_POOL_NAME+"-workers", false));
        }
    }

    protected ExecutorService createMainExecutor(){
        if(backgroundThread == null) {
            int poolSize = 1;
            if (tasks.get(TaskOrder.TASK) != null) {
                poolSize = Math.max(1, tasks.get(TaskOrder.TASK).size());
            }
            backgroundThread = executorServiceFactory.newExecutor(serviceName, poolSize, false);
        }
        return backgroundThread;
    }

    protected abstract void invoke(Task task) throws RejectedExecutionException;

    protected void invokeAll(Collection<? extends Task> tasks) throws RejectedExecutionException {
        tasks.forEach(this::invoke);
    }

    private boolean isEnabled() throws IllegalStateException {
        //or throw Exception instead of return false
        return !disabled;
    }

    private boolean setStarted(){
        if (started) {
            log.warn("The {} dispatcher already started.", serviceName);
            return false;
        }
        started=true;
        return started;
    }

    private boolean isStarted() throws IllegalStateException {
        //or throw Exception instead of return false
        return started;
    }

    @Override
    public boolean schedule(Task task, TaskOrder position) {
        if (isEnabled()) {
            Objects.requireNonNull(task, "Task is NULL.");
            Objects.requireNonNull(position, "Task position is NULL.");
            if (isStarted())
                return false;
            else if (validate(task)) {
                return tasks.put(position, task);
            }else {
                throw new IllegalArgumentException(String.format("The task contains wrong field values, task=%s", task.toString()));
            }
        } else {
            return false;
        }
    }

    @Override
    public void dispatch() {
        if (isEnabled()) {
            synchronized (taskMonitor) {
                if (!setStarted()) return;
                log.debug("Prepare dispatcher {} to start . . .", this.serviceName);

                createMainExecutor();

                Thread thread = new Thread(() -> {
                    Collection<Task> jobs;
                    if (log.isTraceEnabled()) {
                        log.trace("ThreadGroup Name: {}", Thread.currentThread().getThreadGroup().getName());
                        log.trace("Thread Name: {} this={}", Thread.currentThread().getName(), Thread.currentThread());
                        log.trace("Parent Thread Name: {}", Thread.currentThread().getThreadGroup().getParent().getName());
                    }

                    /* Run INIT tasks */
                    jobs = tasks.get(TaskOrder.INIT);
                    log.debug("{}: run INIT tasks.", serviceName);
                    runAllAndWait(jobs);
                    tasks.replaceValues(TaskOrder.INIT, Collections.emptyList());

                    /* Run BEFORE tasks */
                    jobs = tasks.get(TaskOrder.BEFORE);
                    log.debug("{}: run BEFORE tasks.", serviceName);
                    runAllAndWait(jobs);
                    tasks.replaceValues(TaskOrder.BEFORE, Collections.emptyList());

                    /* Run background tasks in thread pool */
                    jobs = tasks.get(TaskOrder.TASK);
                    log.info("{}: run tasks.", serviceName);
                    try {
                        invokeAll(jobs);
                    } catch (RejectedExecutionException e) {
                        throw new IllegalStateException("The background tasks can't be initialized properly.", e);
                    }
                    tasks.replaceValues(TaskOrder.TASK, Collections.emptyList());

                    jobs = tasks.get(TaskOrder.AFTER);
                    log.info("{}: run AFTER tasks.", serviceName);
                    runAllAndWait(jobs);
                    tasks.replaceValues(TaskOrder.AFTER, Collections.emptyList());
                });
                thread.setDaemon(false);
                thread.setName(APL_POOL_NAME+"-"+getServiceName()+"-onStart");
                thread.start();
                log.debug("Dispatcher {} started, thread={}", this.serviceName, thread.toString());
            }
        }
    }

    @Override
    public void shutdown() {
        if (isEnabled()) {
            if (!started) {
                log.warn("The {} dispatcher is not started.", serviceName);
            } else {
                if (backgroundThread != null) {
                    backgroundThread.shutdown();
                    try {
                        backgroundThread.awaitTermination(500, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }

    protected void runAllAndWait(final Collection<Task> tasks) {
        if (tasks != null && tasks.size() > 0) {
            StringBuffer errors = new StringBuffer();
            log.debug("Init CountDownLatch({})", tasks.size());
            final CountDownLatch latch = new CountDownLatch(tasks.size());
            try {
                tasks.forEach(task -> {
                    onStartExecutor.submit(() -> {
                        try {
                            log.trace("Current thread={}", Thread.currentThread().getName());
                            task.getTask().run();
                        } catch (Throwable t) {
                            errors.append(t.getMessage()).append('\n');
                            throw t;
                        } finally {
                            latch.countDown();
                            log.trace("call countDown()");
                        }

                    }, task.getName());
                    log.trace("{} thread started.", task.getName());
                });
            }catch (Throwable e){
                log.error("Unexpected Exception in runAndWait:", e);
            }
            try {
                latch.await();
            } catch (InterruptedException e) {
                log.trace("RunAndWait awaiting interrupted.");
                Thread.currentThread().interrupt();
            }
            if (errors.length() > 0) {
                throw new RuntimeException("Errors running startup tasks:\n" + errors.toString());
            }
        }
    }

    public String info(){
        StringBuilder info = new StringBuilder("Dispatcher=");
        info.append("Name: ").append(serviceName);
        info.append(",Tasks(");
        info.append("INIT=").append(getTasksCount(TaskOrder.INIT));
        info.append(", BEFORE=").append(getTasksCount(TaskOrder.BEFORE));
        info.append(", TASK=").append(getTasksCount(TaskOrder.TASK));
        info.append(", AFTER=").append(getTasksCount(TaskOrder.AFTER));
        info.append(")");
        return info.toString();
    }

    private int getTasksCount(TaskOrder order){
        return tasks.containsKey(order)?tasks.get(order).size():0;
    }

    public static class ScheduledExecutorServiceFactory implements ExecutorServiceFactory {
        @Override
        public ExecutorService newExecutor(String poolName, int poolSize, boolean daemon) {
            return Executors.newScheduledThreadPool(poolSize, new NamedThreadFactory(new ThreadGroup(APL_BKG_WORKERS), poolName, daemon));
        }
    }

}
