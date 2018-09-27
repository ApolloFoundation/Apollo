/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.AccountKey;
import com.apollocurrency.aplwallet.apl.GeneratedAccount;
import com.apollocurrency.aplwallet.apl.NodeClient;
import com.apollocurrency.aplwallet.apl.TestData;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import util.WalletRunner;

import java.io.IOException;

public class ExportKeyTest {
    private static WalletRunner runner = new WalletRunner(true);
    private static final NodeClient nodeClient = new NodeClient();
    private static final String PASSPHRASE = "mypassphrase";
    @AfterClass
    public static void tearDown() throws Exception {
        runner.shutdown();
    }

    @BeforeClass
    public static void setUp() throws Exception {
        runner.run();
    }

    @Test
    public void testExportKey() throws IOException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TestData.TEST_LOCALHOST, PASSPHRASE);
        AccountKey accountKey = nodeClient.exportKey(TestData.TEST_LOCALHOST, PASSPHRASE, Convert.rsAccount(generatedAccount.getId()));
        Assert.assertEquals(PASSPHRASE, generatedAccount.getPassphrase());
        Assert.assertEquals(Convert.rsAccount(generatedAccount.getId()), accountKey.getAccount());
    }

    @Test(expected = RuntimeException.class)
    public void testExportKeyInvalidAccount() throws IOException {
        nodeClient.exportKey(TestData.TEST_LOCALHOST, PASSPHRASE, Convert.rsAccount(1));
    }
    @Test(expected = RuntimeException.class)
    public void testExportKeyInvalidPassphrase() throws IOException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TestData.TEST_LOCALHOST, PASSPHRASE);
        nodeClient.exportKey(TestData.TEST_LOCALHOST, "anotherpassphrase", Convert.rsAccount(generatedAccount.getId()));
    }
}
