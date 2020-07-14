/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable.limiter;

import com.google.common.util.concurrent.TimeLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeLimiterServiceImplTest {

    private TimeLimiterService limiterService;

    @BeforeEach
    void setUp() {
        limiterService = new TimeLimiterServiceImpl();
    }

    @Test
    void acquireLimiter() {
        String name = "FIRST_LIMITER";
        assertNull(limiterService.getExecutor(name));
        TimeLimiter limiter = limiterService.acquireLimiter(name);
        assertNotNull(limiter);
        assertNotNull(limiterService.getExecutor(name));
        assertEquals(limiter, limiterService.acquireLimiter(name));
    }

    @Test
    void acquireLimiter_throw_exception_after_shutdown() {
        limiterService.shutdown();
        assertThrows(IllegalStateException.class, () -> limiterService.acquireLimiter("NAME"));
    }

    @Test
    void shutdown() {
        String name = "FIRST_LIMITER";
        TimeLimiter limiter = limiterService.acquireLimiter(name);
        assertNotNull(limiter);
        limiterService.shutdown();
        ExecutorService executor = limiterService.getExecutor(name);
        assertNotNull(executor);
        assertTrue(executor.isShutdown());
    }

}
