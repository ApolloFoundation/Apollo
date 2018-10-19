/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.junit.Assert;
import org.junit.Test;

public class LegacyAccountGeneratorTest {
    private AccountGenerator accountGenerator = new LegacyAccountGenerator();
    //    private static final GeneratedAccount GENERATED_ACCOUNT = new GeneratedAccount()
    private static final String MESSAGE = "Test message";
    private String PASSPHRASE = "some passphrase";

    @Test
    public void testGenerateAccount() {
        GeneratedAccount actualAcc = accountGenerator.generate(PASSPHRASE);
        byte[] keySeed = Crypto.getKeySeed(actualAcc.getSecretBytes());
        Assert.assertArrayEquals(actualAcc.getPrivateKey(), Crypto.getPrivateKey(keySeed));
        Assert.assertArrayEquals(actualAcc.getPublicKey(), Crypto.getPublicKey(keySeed));
        byte[] signature = Crypto.sign(MESSAGE.getBytes(), actualAcc.getPrivateKey());
        Assert.assertTrue(Crypto.verify(signature, MESSAGE.getBytes(), actualAcc.getPublicKey()));
        Assert.assertEquals(PASSPHRASE, actualAcc.getPassphrase());
        Assert.assertEquals(actualAcc.getId(), Convert.getId(actualAcc.getPublicKey()));
    }
    @Test(expected = RuntimeException.class)
    public void testGenerateAccountNullPassphrase() {
        accountGenerator.generate(null);
    }

}
