/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * POST request
 */
class PostRequest {

    /** Request latch */
    private final CountDownLatch latch = new CountDownLatch(1);
    /** Response message */
    private volatile String response;
    /** Socket exception */
    private volatile IOException exception;
    private final PeerWebSocket outer;

    /**
     * Create a post request
     */
    public PostRequest(final PeerWebSocket outer) {
        this.outer = outer;
    }

    /**
     * Wait for the response
     *
     * The caller must hold the lock for the request condition
     *
     * @param   timeout                 Wait timeout
     * @param   unit                    Time unit
     * @return                          Response message
     * @throws  InterruptedException    Wait interrupted
     * @throws  IOException             I/O error occurred
     */
    public String get(long timeout, TimeUnit unit) throws InterruptedException, IOException {
        if (!latch.await(timeout, unit)) {
            throw new SocketTimeoutException("WebSocket read timeout exceeded");
        }
        if (exception != null) {
            throw exception;
        }
        return response;
    }

    /**
     * Complete the request with a response message
     *
     * The caller must hold the lock for the request condition
     *
     * @param   response                Response message
     */
    public void complete(String response) {
        this.response = response;
        latch.countDown();
    }

    /**
     * Complete the request with an exception
     *
     * The caller must hold the lock for the request condition
     *
     * @param   exception             I/O exception
     */
    public void complete(IOException exception) {
        this.exception = exception;
        latch.countDown();
    }
    
}
