/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.NodeClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import util.WalletRunner;

import java.util.concurrent.TimeUnit;

public abstract class APITest {
        private static final Logger LOG = getLogger(APITest.class);

    protected static WalletRunner runner = new WalletRunner(true);
    protected static final NodeClient nodeClient = new NodeClient();
    protected static final String PASSPHRASE = "mypassphrase";
    @AfterClass
    public static void shutdown() throws Exception {
        LOG.info("===== Shutdown wallet");

        runner.shutdown();
        runner.enableReloading();
        TimeUnit.SECONDS.sleep(5);
    }

    @BeforeClass
    public static void init() throws Exception {
        LOG.info("===== Start wallet");
        runner.disableReloading();
        runner.run();
        TimeUnit.SECONDS.sleep(5);
    }
}
