/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.NodeClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import util.WalletRunner;

public abstract class APITest {
    protected static WalletRunner runner = new WalletRunner();
    protected static final NodeClient nodeClient = new NodeClient();
    protected static final String PASSPHRASE = "mypassphrase";
    @AfterClass
    public static void shutdown() throws Exception {
        runner.shutdown();
    }

    @BeforeClass
    public static void init() throws Exception {
        runner.run();
    }
}
