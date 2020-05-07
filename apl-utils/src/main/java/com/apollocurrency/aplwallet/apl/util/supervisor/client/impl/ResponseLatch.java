/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.supervisor.client.impl;

import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvBusResponse;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketTimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Latch to response waiting
 *
 * @author alukin@gmail.com
 */
@Slf4j
public class ResponseLatch {

    /**
     * time to live of entry. Entry should de deleted if it is older
     */
    public static long WSW_TTL_MS = 60000; //1 minute
    private final CountDownLatch latch = new CountDownLatch(1);
    private final long createTime = System.currentTimeMillis();
    /**
     * Response message
     */
    private volatile SvBusResponse response;

    /**
     * Wait for the response
     * <p>
     * The caller must hold the lock for the request condition
     *
     * @param timeoutMs Wait timeout
     * @return Response message
     * @throws java.net.SocketTimeoutException
     */
    public SvBusResponse get(long timeoutMs) throws SocketTimeoutException {
        try {
            if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                throw new SocketTimeoutException("WebSocket response wait timeout (" + timeoutMs + "ms) exceeded");
            }
        } catch (InterruptedException ex) {
            log.debug("Interruptrd exception while waiting for response", ex);
            Thread.currentThread().interrupt();
        }
        return response;
    }

    public void setResponse(SvBusResponse response) {
        this.response = response;
        latch.countDown();
    }

    public boolean isOld() {
        long now = System.currentTimeMillis();
        boolean res = (now - createTime) > WSW_TTL_MS;
        return res;
    }
}
