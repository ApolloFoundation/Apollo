package com.apollocurrency.aplwallet.apl.util.task;

public class TaskDispatcherFactory {

    public static TaskDispatcher newBackgroundDispatcher(String serviceName){
        return new BackgroundTaskDispatcher(serviceName);
    }

    public static TaskDispatcher newScheduledDispatcher(String serviceName){
        return new ScheduledTaskDispatcher(serviceName);
    }


}
