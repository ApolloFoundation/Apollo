package com.apollocurrency.aplwallet.apl.util.task;

import java.util.concurrent.ExecutorService;

public interface TaskExecutorService {

    boolean validate(Task task);

    void invoke(Task task);

    ExecutorService executor();
}
