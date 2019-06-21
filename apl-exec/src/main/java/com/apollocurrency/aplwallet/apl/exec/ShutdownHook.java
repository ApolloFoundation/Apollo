package com.apollocurrency.aplwallet.apl.exec;

import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.core.app.FundingMonitor;
import com.apollocurrency.aplwallet.apl.core.app.service.SecureStorageService;
import org.slf4j.Logger;

import javax.enterprise.inject.spi.CDI;

import static org.slf4j.LoggerFactory.getLogger;

public class ShutdownHook extends Thread  {
    private static final Logger LOG = getLogger(FundingMonitor.class);

    private AplCoreRuntime aplCoreRuntime;
    private SecureStorageService secureStorageService;

    public ShutdownHook() {
        super("ShutdownHookThread");
        this.aplCoreRuntime = CDI.current().select(AplCoreRuntime.class).get();
        this.secureStorageService = CDI.current().select(SecureStorageService.class).get();
    }

    @Override
    public void run() {

    }
}
