package com.apollocurrency.aplwallet.apl.exec;

import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.core.app.FundingMonitor;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class ShutdownHook extends Thread {
    private static final Logger LOG = getLogger(FundingMonitor.class);

    private AplCoreRuntime aplCoreRuntime;

    /**
     * Explicit constructor with paramaters.
     *
     * @param aplCoreRuntime instance is proxied by CDI
     */
    public ShutdownHook(AplCoreRuntime aplCoreRuntime) {
        super("ShutdownHookThread");
        this.aplCoreRuntime = aplCoreRuntime;
    }

    @Override
    public void run() {

        try {
            aplCoreRuntime.shutdown();
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }

        Apollo.shutdownWeldContainer();
    }
}
