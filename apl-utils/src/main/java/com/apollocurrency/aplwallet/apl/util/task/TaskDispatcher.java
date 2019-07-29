/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.task;

public interface TaskDispatcher {

    default boolean schedule(Task task){
        return schedule(task, TaskOrder.TASK);
    }

    boolean schedule(Task task, TaskOrder order);

    boolean cancel(String name);

    boolean cancelAll();

    void shutdown();

}
