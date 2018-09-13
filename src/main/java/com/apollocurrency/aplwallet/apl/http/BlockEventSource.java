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
    private static final QueuedThreadPool pool = new QueuedThreadPool(1000, 100);
    private volatile boolean shutdown = false;
static {
    try {
        pool.setIdleTimeout(30000);
        pool.setName("SSE blocks pool");
        pool.start();
    } catch (Exception e) {
        LOG.error(e.toString(), e);
    }
}
    @Override
    public void onOpen(Emitter emitter) throws IOException {

        this.emitter = emitter;
        emitter.data("SSE data");
        pool.execute(() -> {
            Blockchain blockchain = Apl.getBlockchain();
            Block block = blockchain.getLastBlock();
            Block currentBlock = block;
            while (!shutdown) {
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    return;
                }
                try {
                    emitEvent(currentBlock.getJSONObject().toJSONString());
                    currentBlock = blockchain.getLastBlock();
                    if (currentBlock.getHeight() > block.getHeight()) {
                        LOG.trace("Writing new block");
                        emitEvent(block.getJSONObject().toJSONString());
                        block = currentBlock;
                    }
                } catch (IOException e) {
                    LOG.error("Unable to send sse event", e);
                    return;
                }
            }
        });
    }

    public void emitEvent(String dataToSend) throws IOException {
        this.emitter.data(dataToSend);
    }

    public BlockEventSource() {


    }

    @Override
    public void onClose() {
        shutdown = true;

    }
}
