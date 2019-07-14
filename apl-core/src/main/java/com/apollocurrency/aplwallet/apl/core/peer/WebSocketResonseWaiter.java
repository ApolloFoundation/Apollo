/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 *
 * @author al
 */
public class WebSocketResonseWaiter {
    /** time to live of entrly. Entry should de deleted if it is older */
    public static long WSW_TTL_MS=60000; //1 minute
      /** Request latch */
    private final CountDownLatch latch = new CountDownLatch(1);
    /** Response message */
    private volatile String response;
    private final long createTime = System.currentTimeMillis();
    
    /**
     * Wait for the response
     *
     * The caller must hold the lock for the request condition
     *
     * @param   timeoutMs                 Wait timeout
     * @return                          Response message
     * @throws  InterruptedException    Wait interrupted
     * @throws  IOException             I/O error occurred
     */
    public String get(long timeoutMs) throws InterruptedException, IOException {
        if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            throw new SocketTimeoutException("WebSocket read timeout exceeded");
        }
        return response;
    }
    public void setResponse(String response) {
        this.response = response;
        latch.countDown();
    }

    public boolean isOld(){
        long now = System.currentTimeMillis();
        boolean res = (now-createTime) > WSW_TTL_MS;
        return res;
    }    
}
