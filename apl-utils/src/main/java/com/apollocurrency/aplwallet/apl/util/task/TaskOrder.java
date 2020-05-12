/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.task;

/**
 * An enum to organize the task execution order.
 * An {@link TaskDispatcher} keeps to this order to execute tasks.
 * <p><ul>
 * <li>{@code INIT} - the initial task, will be run first
 * <li>{@code BEFORE} - this task will be run before the main task
 * <li>{@code TASK} - this is the main periodical task or tasks
 * <li>{@code AFTER} - this task will be run after the main task is started
 * <ul/>
 */
public enum TaskOrder {
    INIT,
    BEFORE,
    TASK,
    AFTER
}
