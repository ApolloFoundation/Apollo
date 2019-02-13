/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import com.apollocurrency.aplwallet.apl.core.account.GeneratedAccount;
import com.apollocurrency.aplwallet.apl.core.app.LegacyAccountGenerator;
import com.apollocurrency.aplwallet.apl.core.account.AccountGenerator;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LegacyAccountGeneratorTest {
    private AccountGenerator accountGenerator = new LegacyAccountGenerator();
    //    private static final GeneratedAccount GENERATED_ACCOUNT = new GeneratedAccount()
    private static final String MESSAGE = "Test message";
    private String PASSPHRASE = "some passphrase";

    @Test
    public void testGenerateAccount() {
        GeneratedAccount actualAcc = accountGenerator.generate(PASSPHRASE);
        byte[] keySeed = Crypto.getKeySeed(actualAcc.getSecretBytes());
        assertArrayEquals(actualAcc.getPrivateKey(), Crypto.getPrivateKey(keySeed));
        assertArrayEquals(actualAcc.getPublicKey(), Crypto.getPublicKey(keySeed));
        byte[] signature = Crypto.sign(MESSAGE.getBytes(), actualAcc.getPrivateKey());
        assertTrue(Crypto.verify(signature, MESSAGE.getBytes(), actualAcc.getPublicKey()));
        assertEquals(PASSPHRASE, actualAcc.getPassphrase());
        assertEquals(actualAcc.getId(), Convert.getId(actualAcc.getPublicKey()));
    }
    @Test
    public void testGenerateAccountNullPassphrase() {
        assertThrows(RuntimeException.class, () -> accountGenerator.generate(null));
    }

}
