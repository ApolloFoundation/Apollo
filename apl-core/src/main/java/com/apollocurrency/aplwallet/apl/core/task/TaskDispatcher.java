/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.task;

public interface TaskDispatcher {

    void dispatch();

    boolean validate(Task task);

    default boolean schedule(Task task){
        return schedule(task, TaskOrder.TASK);
    }

    boolean schedule(Task task, TaskOrder order);

    void shutdown();

    String info();

}
