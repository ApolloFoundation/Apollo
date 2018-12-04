/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import static com.apollocurrency.aplwallet.apl.TestConstants.TEST_LOCALHOST;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;

import java.io.IOException;
import java.util.Random;

import com.apollocurrency.aplwallet.apl.KeyStore;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.junit.Assert;
import org.junit.Test;
import util.TestUtil;

public class ImportKeyTest extends DeleteGeneratedAccountsTest {
    private static Random random = new Random();
    public byte[] generateSecretBytes(int size) {
        byte[] secretBytes = new byte[32];
        random.nextBytes(secretBytes);
        return secretBytes;
    }

    @Test
    public void testImportKey() throws IOException {
        byte[] secretBytes = generateSecretBytes(32);
        String passphrase = nodeClient.importKey(TEST_LOCALHOST, PASSPHRASE, Convert.toHexString(secretBytes));
        generatedAccounts.add(Convert.rsAccount(Convert.getId(Crypto.getPublicKey(secretBytes))));

        Assert.assertEquals(PASSPHRASE, passphrase);
    }
    @Test
    public void testImportKeyWithoutPassphrase() throws IOException {
        importWithoutPassphrase();
    }

    @Test
    public void testImportKeyAlreadyImported() throws IOException {
        byte[] secretBytes = importWithoutPassphrase();

        String json = nodeClient.importKeyJson(TEST_LOCALHOST, null, Convert.toHexString(secretBytes));
        assertThatJson(json)
                .isPresent()
                .node("errorDescription")
                .isPresent()
                .matches(TestUtil.createStringMatcher(KeyStore.Status.DUPLICATE_FOUND.message));
    }

    private byte[] importWithoutPassphrase() throws IOException {
        byte[] secretBytes = generateSecretBytes(32);

        String passphrase = nodeClient.importKey(TEST_LOCALHOST, null, Convert.toHexString(secretBytes));
        generatedAccounts.add(Convert.rsAccount(Convert.getId(Crypto.getPublicKey(Crypto.getKeySeed(secretBytes)))));
        Assert.assertNotEquals(PASSPHRASE, passphrase);
        String[] passphraseWords = passphrase.split(" ");
        Assert.assertTrue(passphraseWords.length >=10 && passphraseWords.length <=15);
        return secretBytes;
    }

}
