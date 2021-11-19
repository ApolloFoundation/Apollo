/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.service;

import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcher;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcherFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.apollocurrency.aplwallet.apl.util.task.DefaultTaskDispatcher.DEFAULT_THREAD_POOL_SIZE;

@Slf4j
public class TaskDispatchManager {

    private final Object dispatchersMonitor = new Object();
    private volatile boolean isTasksStarted = false;
    private PropertiesHolder propertiesHolder;
    private Map<String, TaskDispatcher> dispatchers;
//just to be proxyable
    public TaskDispatchManager() {
    }

    public TaskDispatchManager(PropertiesHolder propertiesHolder) {
        this.propertiesHolder = propertiesHolder;
        dispatchers = new HashMap<>();
    }

    public TaskDispatcher newScheduledDispatcher(String serviceName) {
        Objects.requireNonNull(serviceName, "serviceName is NULL");
        TaskDispatcher dispatcher = TaskDispatcherFactory.newScheduledDispatcher(serviceName);
        registerDispatcher(dispatcher, serviceName);

        return dispatcher;
    }

    public TaskDispatcher newBackgroundDispatcher(String serviceName) {
        Objects.requireNonNull(serviceName, "serviceName is NULL");
        TaskDispatcher dispatcher = TaskDispatcherFactory.newBackgroundDispatcher(serviceName);
        registerDispatcher(dispatcher, serviceName);

        return dispatcher;
    }

    public void dispatch() {
        log.debug("Starting all registered dispatchers ...");
        if (log.isTraceEnabled()) {
            dispatchers.values().forEach(dispatcher -> log.trace(dispatcher.info()));
        }
        synchronized (dispatchersMonitor) {
            isTasksStarted = true;
            dispatchers.values().forEach(TaskDispatcher::dispatch);
        }
    }

    public void shutdown() {
        log.debug("Shutdown all registered dispatchers ...");
        synchronized (dispatchersMonitor) {
            dispatchers.values().forEach(TaskDispatcher::shutdown);
            dispatchers.clear();
        }
    }

    private void registerDispatcher(TaskDispatcher dispatcher, String name) {
        log.trace("Register {} service ", name);

        synchronized (dispatchersMonitor) {
            if (isTasksStarted) {
                log.error("DispatcherManager has been started all registered tasks. TaskDispatcher {} has wrong register time.", name);
            }

            dispatchers.put(name, dispatcher);
        }
    }

    /**
     * Remove and shutdown the dispatcher
     *
     * @param dispatcher
     */
    public void removeDispatcher(TaskDispatcher dispatcher) {
        removeDispatcher(dispatcher, true);
    }

    public void removeDispatcher(TaskDispatcher dispatcher, boolean shutdown) {
        boolean found = false;
        synchronized (dispatchersMonitor) {
            if (dispatchers.containsValue(dispatcher)) {
                found = dispatchers.values().removeIf(value -> value.equals(dispatcher));
            }
        }
        if (found && shutdown) {
            dispatcher.shutdown();
        }
    }

    /**
     * Remove and shutdown dispatcher by name
     *
     * @param name a dispatcher name
     */
    public void removeDispatcher(String name) {
        removeDispatcher(name, true);
    }

    public void removeDispatcher(String name, boolean shutdown) {
        TaskDispatcher dispatcher;
        synchronized (dispatchersMonitor) {
            dispatcher = dispatchers.remove(name);
        }
        if (dispatcher != null && shutdown) {
            dispatcher.shutdown();
        }
    }

    public TaskDispatcher getDispatcher(String serviceName) {
        synchronized (dispatchersMonitor) {
            return dispatchers.get(serviceName);
        }
    }

    private boolean retriveDisabledValue(String serviceName) {
        return propertiesHolder.getBooleanProperty(String.format("apl.disable%sThread", serviceName), false);
    }

    private int retrivePoolSize(String serviceName) {//TODO: this property need to be analyzed
        return propertiesHolder.getIntProperty(String.format("apl.%sThreadPoolSizeMAX", serviceName), DEFAULT_THREAD_POOL_SIZE);
    }

}
