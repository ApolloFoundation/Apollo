/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.inttest.http;

import com.apollocurrency.aplwallet.apl.inttest.core.NodeClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import com.apollocurrency.aplwallet.apl.inttest.util.WalletRunner;

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
