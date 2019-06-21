package com.apollocurrency.aplwallet.apl.exec;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.core.app.FundingMonitor;
import com.apollocurrency.aplwallet.apl.core.app.service.SecureStorageService;
import org.slf4j.Logger;

public class ShutdownHook extends Thread  {
    private static final Logger LOG = getLogger(FundingMonitor.class);

    private AplCoreRuntime aplCoreRuntime;
    private SecureStorageService secureStorageService;

    /**
     * Explicit constructor with paramaters.
     *
     * @param aplCoreRuntime instance is proxied by CDI
     * @param secureStorageService instance is be proxied by CDI
     */
    public ShutdownHook(AplCoreRuntime aplCoreRuntime, SecureStorageService secureStorageService) {
        super("ShutdownHookThread");
        this.aplCoreRuntime = aplCoreRuntime;
        this.secureStorageService = secureStorageService;
    }

    @Override
    public void run() {
        try {
            secureStorageService.storeSecretStorage();
        }catch (Exception ex){
            LOG.error(ex.getMessage(), ex);
        }

        try {
            aplCoreRuntime.shutdown();
        } catch (Exception ex){
            LOG.error(ex.getMessage(), ex);
        }

        Apollo.shutdownWeldContainer();
    }
}
