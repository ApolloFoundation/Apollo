/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.Block;
import com.apollocurrency.aplwallet.apl.Blockchain;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public class BlockEventSource implements org.eclipse.jetty.servlets.EventSource {
    public static final Logger LOG = getLogger(BlockEventSource.class);
    private Emitter emitter;
    private volatile boolean shutdown = false;
    private ThreadPoolExecutor threadPoolExecutor;
    private long accountId;
    @Override
    public void onOpen(Emitter emitter) throws IOException {
        this.emitter = emitter;
        threadPoolExecutor.execute(new BlockEventSourceProcessor(this, accountId));
    }

    public void emitEvent(String dataToSend) throws IOException {
        this.emitter.data(dataToSend);
    }

    @Override
    public void onClose() {
        LOG.trace("Close event source");
        shutdown = true;

    }

    public BlockEventSource(ThreadPoolExecutor threadPoolExecutor, long accountId) {
        this.threadPoolExecutor = threadPoolExecutor;
        this.accountId = accountId;
    }

    public boolean isShutdown() {
        return shutdown;
    }
}
