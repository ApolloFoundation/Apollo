/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http;

import org.slf4j.Logger;

import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;

import static org.slf4j.LoggerFactory.getLogger;

public class BlockEventSource /*implements org.eclipse.jetty.servlets.EventSource*/ {
    public static final Logger LOG = getLogger(BlockEventSource.class);
//    private Emitter emitter;
    private volatile boolean shutdown = false;
    private ThreadPoolExecutor threadPoolExecutor;
    private long accountId;

    public BlockEventSource(ThreadPoolExecutor threadPoolExecutor, long accountId) {
        this.threadPoolExecutor = threadPoolExecutor;
        this.accountId = accountId;
    }

//    @Override
//    public void onOpen(Emitter emitter) throws IOException {
    public void onOpen(Object emitter) throws IOException {
//        this.emitter = emitter;
        threadPoolExecutor.execute(new BlockEventSourceProcessor(this, accountId));
    }

    public void emitEvent(String dataToSend) throws IOException {
//        this.emitter.data(dataToSend);
    }

//    @Override
    public void onClose() {
        LOG.trace("Close event source");
        shutdown = true;

    }

    public boolean isShutdown() {
        return shutdown;
    }
}
