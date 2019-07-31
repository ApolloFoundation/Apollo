/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.task;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ScheduledTaskDispatcher extends AbstractTaskDispatcher {

    public ScheduledTaskDispatcher(String serviceName) {
        super(new ScheduledExecutorServiceFactory(), serviceName);
    }

    @Override
    public boolean validate(Task task) {
        return !(task.getTask() == null || task.getName() == null || task.getDelay()<=0);
    }

    @Override
    public void invoke(Task task) throws RejectedExecutionException {
        try {
            ((ScheduledExecutorService) createMainExecutor()).scheduleAtFixedRate(
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
