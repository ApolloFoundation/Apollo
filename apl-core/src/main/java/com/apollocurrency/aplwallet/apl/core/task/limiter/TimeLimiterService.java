/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.task.limiter;

import com.google.common.util.concurrent.TimeLimiter;

import java.util.concurrent.ExecutorService;

public interface TimeLimiterService {

    TimeLimiter acquireLimiter(String name);

    ExecutorService getExecutor(String name);

    void shutdown();

}
