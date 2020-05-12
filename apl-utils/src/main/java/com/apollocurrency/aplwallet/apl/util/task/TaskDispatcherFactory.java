package com.apollocurrency.aplwallet.apl.util.task;

/**
 * The factory class to create a dispatcher
 */
public class TaskDispatcherFactory {

    /**
     * Return the new scheduled dispatcher, that runs background tasks with fixed delay
     *
     * @param serviceName the service name
     * @return the new dispatcher
     */
    public static TaskDispatcher newBackgroundDispatcher(String serviceName) {
        return new DefaultTaskDispatcher(new DefaultTaskDispatcher.BackgroundExecutorServiceFactory(), serviceName);
    }

    /**
     * Return the new scheduled dispatcher, that runs background tasks at fixed rate
     *
     * @param serviceName the service name
     * @return the new dispatcher
     */
    public static TaskDispatcher newScheduledDispatcher(String serviceName) {
        return new DefaultTaskDispatcher(new DefaultTaskDispatcher.ScheduledExecutorServiceFactory(), serviceName);
    }

}
