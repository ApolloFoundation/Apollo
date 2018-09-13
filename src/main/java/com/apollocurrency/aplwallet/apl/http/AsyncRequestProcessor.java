/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.Block;
import com.apollocurrency.aplwallet.apl.Blockchain;
import org.slf4j.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public class AsyncRequestProcessor implements Runnable {
    private static final Logger LOG = getLogger(AsyncRequestProcessor.class);

    private AsyncContext asyncContext;

    public AsyncRequestProcessor() {
    }

    public AsyncRequestProcessor(AsyncContext asyncCtx) {
        this.asyncContext = asyncCtx;
    }

    @Override
    public void run() {
        LOG.info("Async Supported? "
                + asyncContext.getRequest().isAsyncSupported());
        try {
            PrintWriter out = asyncContext.getResponse().getWriter();
            ServletResponse response = asyncContext.getResponse();
            response.setContentType("text/event-stream");
            response.setCharacterEncoding("UTF-8");
            Blockchain blockchain = Apl.getBlockchain();
            Block block = blockchain.getLastBlock();
            out.write("data: " + block.getStringId() + "\n\n");
            out.flush();
            while (true) {
                try {
                    TimeUnit.SECONDS.sleep(2);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Block currentBlock = blockchain.getLastBlock();
                LOG.trace("Curent block: " + block.getId());
                if (currentBlock.getHeight() > block.getHeight()) {
                    LOG.trace("Writing new block");
            out.write("data: " + currentBlock.getStringId() + "\n\n");

                    block = currentBlock;
                    out.flush();
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        //complete the processing
        asyncContext.complete();
    }

    private void longProcessing(int secs) {
        // wait for given time before finishing
        try {
            Thread.sleep(secs);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}