/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.task;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static com.apollocurrency.aplwallet.apl.util.task.Tasks.shutdownExecutor;

/**
 * The default {@link TaskDispatcher} that manages the background tasks
 */
@Slf4j
public class DefaultTaskDispatcher implements TaskDispatcher {

    public static final int DEFAULT_THREAD_POOL_SIZE = 10;
    public static final String APL_BG_WORKERS = "apl-bg-workers";
    public static final String APL_POOL_NAME = "apl-dispatcher";

    private static final AtomicInteger groupNumber = new AtomicInteger(1);

    @Getter
    protected final Map<String,String> initParameters;

    protected ExecutorServiceFactory executorServiceFactory;
    /**
     * The main executor
     */
    protected TaskExecutorService backgroundThread;
    @Getter
    protected String serviceName;

    protected volatile boolean started = false;

    /**
     * The executor that runs INIT and BEFORE tasks
     */
    private final ExecutorService onStartExecutor;

    /**
     * The tasks queue
     */
    private final HashMap<TaskOrder, List<Task>> tasks = new HashMap<>();

    private final Object taskMonitor = new Object();

    /**
     * Create dispatcher with initial parameters
     * @param executorServiceFactory the factory
     * @param name the dispatcher name
     */
    public DefaultTaskDispatcher(ExecutorServiceFactory executorServiceFactory, String name) {
        this.executorServiceFactory = Objects.requireNonNull(executorServiceFactory, "ExecutorFactory is NULL");
        this.serviceName = APL_BG_WORKERS +"-"+Objects.requireNonNull(name, "Service name is NULL");
        this.initParameters = new HashMap<>(3);
        this.onStartExecutor = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE,
                    new NamedThreadFactory(new ThreadGroup(APL_BG_WORKERS), APL_POOL_NAME+"-workers", false));
    }

    @Override
    public String getName(){
        return serviceName;
    }

    /**
     * Create main executor for all periodical background tasks
     * @return
     */
    protected TaskExecutorService createMainExecutor(){
        if(backgroundThread == null) {
            //TODO: adjust code using init parameters and min/max constraints
            //String corePoolSize = getInitParameter(TaskInitParameters.APL_CORE_POOL_SIZE);
            //String maxPoolSize = getInitParameter(TaskInitParameters.APL_MAX_POOL_SIZE);
            int poolSize = 1;
            if (tasks.get(TaskOrder.TASK) != null) {
                poolSize = Math.max(1, tasks.get(TaskOrder.TASK).size());
            }
            backgroundThread = executorServiceFactory.newExecutor(serviceName, poolSize, false);
        }
        return backgroundThread;
    }

    @Override
    public boolean validate(Task task) {
        return createMainExecutor().validate(task);
    }

    @Override
    public void invoke(Task task) throws RejectedExecutionException{
        if (task == null) {
            log.debug("SKIPPING, task is 'NULL'");
            return;
        }
        createMainExecutor().invoke(task);
    }

    /**
     * Submit all tasks from collection
     * @param tasks the collection of tasks to submit
     * @throws RejectedExecutionException
     */
    protected void invokeAll(Collection<? extends Task> tasks) throws RejectedExecutionException {
        log.debug("invokeAll on [{}] prepared task(s)", tasks != null ? tasks.size() : -1);
        if (tasks == null || tasks.size() <= 0) {
            log.debug("SKIPPING, empty/null task collection.");
            return;
        }
        tasks.forEach(this::invoke);
    }

    @Override
    public void suspend() {
        log.debug("Suspend dispatcher={}", serviceName);
        createMainExecutor().suspend();
    }

    @Override
    public void resume() {
        log.debug("Resume dispatcher={}", serviceName);
        createMainExecutor().resume();
    }

    @Override
    public ExecutorService executor() {
        return createMainExecutor().executor();
    }

    @Override
    public boolean schedule(Task task, TaskOrder position) {
        Objects.requireNonNull(task, "Task is NULL.");
        Objects.requireNonNull(position, "Task position is NULL.");
        if (isStarted()) {
            if (isShutdown()) {
                throw new RejectedExecutionException("Dispatcher is shutdown.");
            }
            return false;
        }else {
            if (validate(task)){
                return put(position, task);
            } else {
                throw new IllegalArgumentException(String.format("The task contains the wrong field values, task=%s", task.toString()));
            }
        }
    }

    /**
     * Put the task into the queue
     * @param position the task order
     * @param task the task to put
     * @return
     */
    private boolean put(TaskOrder position, Task task){
        List<Task> taskList = tasks.getOrDefault(position, new ArrayList<>());
        taskList.add(task);
        tasks.put(position, taskList);
        return true;
    }

    @Override
    public void dispatch() {
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
                try {
                    runAllAndWait(jobs);
                }catch (Throwable e){
                    log.error("The INIT tasks can't be initialized properly. Jobs = " + jobs, e);
                }
                tasks.remove(TaskOrder.INIT);

                /* Run BEFORE tasks */
                jobs = tasks.get(TaskOrder.BEFORE);
                log.debug("{}: run BEFORE tasks.", serviceName);
                runAllAndWait(jobs);
                tasks.remove(TaskOrder.BEFORE);

                /* Run background tasks in thread pool */
                jobs = tasks.get(TaskOrder.TASK);
                log.info("{}: run tasks.", serviceName);
                try {
                    invokeAll(jobs);
                } catch (RejectedExecutionException e) {
                    throw new IllegalStateException("The background tasks can't be initialized properly.", e);
                }
                tasks.remove(TaskOrder.TASK);

                jobs = tasks.get(TaskOrder.AFTER);
                log.info("{}: run AFTER tasks.", serviceName);
                try {
                    runAllAndWait(jobs);
                }catch (Throwable e){
                    log.error("The AFTER tasks can't be initialized properly.", e);
                }
                tasks.remove(TaskOrder.AFTER);

                //Shutdown onStartExecutor to release resources
                onStartExecutor.shutdown();

            });
            thread.setDaemon(false);
            thread.setName(APL_POOL_NAME + "-" + getServiceName() + "-onStart");
            thread.start();
            log.debug("Dispatcher {} started, thread={}", this.serviceName, thread.toString());
        }

    }

    @Override
    public void shutdown() {
        if (!started) {
            log.warn("The {} dispatcher is not started.", serviceName);
        } else {
            if (backgroundThread != null) {
                shutdownExecutor(serviceName, backgroundThread.executor(), 2);
            }
        }
    }

    @Override
    public boolean isShutdown() {
        if (backgroundThread != null) {
            return backgroundThread.executor().isShutdown();
        }
        return false;
    }

    /**
     * Set dispatcher to the Started state
     * @return false if dispatcher is already started otherwise return true
     */
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

    /**
     * Runs all tasks from the collection and Blocks until all tasks have completed execution, or
     * the timeout occurs, or the current thread is interrupted, whichever happens first.
     * @param tasks the tasks collection to run
     */
    private void runAllAndWait(final Collection<Task> tasks) {
        if (tasks != null && tasks.size() > 0) {
            StringBuffer errors = new StringBuffer();
            log.debug("Init CountDownLatch({})", tasks.size());
            final CountDownLatch latch = new CountDownLatch(tasks.size());
            try {
                tasks.forEach(task -> {
                    onStartExecutor.submit(() -> {
                        try {
                            log.debug("Task = {}, Current thread={}", task.getDelay(), Thread.currentThread().getName());
                            task.getTask().run();
                        } catch (Throwable t) {
                            errors.append(" Task=" + task.getName() + ", " + t.getMessage()).append('\n');
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

    @Override
    public String info(){
        StringBuilder info = new StringBuilder("Dispatcher={");
        info.append("name:").append(serviceName);
        info.append(",tasks:[");
        info.append("INIT:").append(getTasksCount(TaskOrder.INIT));
        info.append(", BEFORE:").append(getTasksCount(TaskOrder.BEFORE));
        info.append(", TASK:").append(getTasksCount(TaskOrder.TASK));
        info.append(", AFTER:").append(getTasksCount(TaskOrder.AFTER));
        info.append("]}");
        return info.toString();
    }

    private int getTasksCount(TaskOrder order){
        return tasks.containsKey(order)?tasks.get(order).size():0;
    }

    public String getInitParameter(String param){
        if (initParameters == null) return null;
        return initParameters.get(param);
    }

    public Enumeration<String> getInitParameterNames(){
        if (initParameters == null) return Collections.enumeration(Collections.EMPTY_LIST);
        return Collections.enumeration(initParameters.keySet());
    }

    public void setInitParameter(String param, String value){
        initParameters.put(param,value);
    }

    public void setInitParameters(Map<String,String> map){
        initParameters.clear();
        initParameters.putAll(map);
    }

    private static String nextGroupName(){
        return APL_BG_WORKERS+"-"+groupNumber.getAndIncrement();
    }

    public static class ScheduledExecutorServiceFactory implements ExecutorServiceFactory {
        @Override
        public TaskExecutorService newExecutor(String poolName, int poolSize, boolean daemon) {
            return  new ScheduledTaskExecutorService(
                    new PausableScheduledThreadPoolExecutor(poolSize,
                            new NamedThreadFactory(new ThreadGroup(nextGroupName()), poolName, daemon)));
        }
    }

    public static class BackgroundExecutorServiceFactory implements ExecutorServiceFactory {
        @Override
        public TaskExecutorService newExecutor(String poolName, int poolSize, boolean daemon) {
            return  new BackgroundTaskExecutorService(
                        new PausableScheduledThreadPoolExecutor(poolSize,
                            new NamedThreadFactory(new ThreadGroup(nextGroupName()), poolName, daemon)));
        }
    }

   /* public static class FixedExecutorServiceFactory implements ExecutorServiceFactory {
        @Override
        public TaskExecutorService newExecutor(String poolName, int poolSize, boolean daemon) {
            return  new FixedSizeTaskExecutorService(
                        new ThreadPoolExecutor(poolSize, poolSize,
                            0L, TimeUnit.MILLISECONDS,
                            new LinkedBlockingQueue<Runnable>(),
                            new NamedThreadFactory(new ThreadGroup(nextGroupName()), poolName, daemon)));
        }
    }*/

    private static class PausableScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor implements PausableExecutorService{
        private boolean isPaused;
        private ReentrantLock pauseLock = new ReentrantLock();
        private Condition condition = pauseLock.newCondition();

        public PausableScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory) {
            super(corePoolSize, threadFactory);
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t, r);
            pauseLock.lock();
            try {
                while (isPaused) condition.await();
            } catch(InterruptedException ie) {
                t.interrupt();
            } finally {
                pauseLock.unlock();
            }
        }

        @Override
        public boolean isPaused() {
            return isPaused;
        }

        @Override
        public void suspend() {
            pauseLock.lock();
            try {
                isPaused = true;
            } finally {
                pauseLock.unlock();
            }
        }

        @Override
        public void resume() {
            pauseLock.lock();
            try {
                isPaused = false;
                condition.signalAll();
            } finally {
                pauseLock.unlock();
            }
        }
    }

    private static abstract class AbstractTaskExecutorService implements TaskExecutorService{
        protected PausableExecutorService service;

        AbstractTaskExecutorService(PausableExecutorService service) {
            this.service = service;
        }

        @Override
        public PausableExecutorService executor() {
            return service;
        }

        @Override
        public void suspend() {
            executor().suspend();
        }

        @Override
        public void resume() {
            executor().resume();
        }
    }

    private static class BackgroundTaskExecutorService extends AbstractTaskExecutorService {

        BackgroundTaskExecutorService(PausableExecutorService service) {
            super(service);
        }

        @Override
        public boolean validate(Task task) {
            return task.getTask() != null && task.getName() != null && task.getInitialDelay() >=0;
        }

        @Override
        public void invoke(Task task) throws RejectedExecutionException {
            try {
                ((ScheduledExecutorService)service).scheduleWithFixedDelay(
                        task,
                        task.getInitialDelay(),
                        task.getDelay(),
                        TimeUnit.MILLISECONDS);
            }catch (Exception e){
                log.error("The task {} can't be scheduled, cause:{}", task.getName(), e.getMessage());
                throw new RejectedExecutionException(e);
            }
        }
    }

    private static class ScheduledTaskExecutorService extends AbstractTaskExecutorService {

        ScheduledTaskExecutorService(PausableExecutorService service) {
            super(service);
        }

        @Override
        public boolean validate(Task task) {
            return task.getTask() != null && task.getName() != null && task.getInitialDelay() >=0 && task.getDelay()>0 ;
        }

        @Override
        public void invoke(Task task) throws RejectedExecutionException {
            try {
                ((ScheduledExecutorService)service).scheduleAtFixedRate(
                        task,
                        task.getInitialDelay(),
                        task.getDelay(),
                        TimeUnit.MILLISECONDS);
            }catch (Exception e){
                log.error("The task {} can't be scheduled, cause:{}", task.getName(), e.getMessage());
                throw new RejectedExecutionException(e);
            }
        }
    }

    private static class FixedSizeTaskExecutorService extends AbstractTaskExecutorService {

        FixedSizeTaskExecutorService(PausableExecutorService service) {
            super(service);
        }
        @Override
        public boolean validate(Task task) {
            return task.getTask() != null && task.getName() != null;
        }

        @Override
        public void invoke(Task task) throws RejectedExecutionException {
            try {
                service.submit(task);
            }catch (Exception e){
                log.error("The task {} can't be submitted, cause:{}", task.getName(), e.getMessage());
                throw new RejectedExecutionException(e);
            }
        }
    }
}
