/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.NodeClient;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import util.WalletRunner;

import java.io.IOException;
import java.util.Random;

import static com.apollocurrency.aplwallet.apl.TestData.TEST_LOCALHOST;

public class ImportKeyTest {
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
        runner.disableReloading();
    }

    @Test
    public void testImportKey() throws IOException {
        Random random = new Random();
        byte[] keySeed = new byte[32];
        random.nextBytes(keySeed);

        String passphrase = nodeClient.importKey(TEST_LOCALHOST, PASSPHRASE, Convert.toHexString(keySeed));

        Assert.assertEquals(PASSPHRASE, passphrase);
    }
    @Test
    public void testImportKeyWithoutPassphrase() throws IOException {
        Random random = new Random();
        byte[] keySeed = new byte[32];
        random.nextBytes(keySeed);

        String passphrase = nodeClient.importKey(TEST_LOCALHOST, null, Convert.toHexString(keySeed));

        Assert.assertNotEquals(PASSPHRASE, passphrase);
        String[] passphraseWords = passphrase.split(" ");
        Assert.assertTrue(passphraseWords.length >=10 && passphraseWords.length <=15);
    }

    @Test(expected = RuntimeException.class)
    public void testImportKeyInvalidKeySeed() throws IOException {
        nodeClient.importKey(TEST_LOCALHOST, null, Convert.toHexString(new byte[10]));
    }
}
