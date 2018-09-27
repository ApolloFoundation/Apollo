/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.GeneratedAccount;
import com.apollocurrency.aplwallet.apl.NodeClient;
import com.apollocurrency.aplwallet.apl.TestData;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import util.WalletRunner;

import java.io.IOException;

public class GenerateAccountTest {
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
    public void testGenerateAccount() throws IOException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TestData.TEST_LOCALHOST, PASSPHRASE);
        Assert.assertEquals(PASSPHRASE, generatedAccount.getPassphrase());
    }

    @Test
    public void testGenerateAccountWithoutPassphrase() throws IOException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TestData.TEST_LOCALHOST, null);
        Assert.assertNotNull(generatedAccount.getPublicKey());
        Assert.assertNotEquals(0, generatedAccount.getId());
        Assert.assertNotEquals(PASSPHRASE, generatedAccount.getPassphrase());
    }
}
