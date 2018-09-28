/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.db;

import com.apollocurrency.aplwallet.apl.DbIntegrationTest;
import org.apache.commons.codec.binary.Base32;
import org.junit.Assert;
import org.junit.Test;

import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.*;


public class TwoFactorAuthRepositoryIntegrationTest extends DbIntegrationTest {
    private TwoFactorAuthRepository repository = new TwoFactorAuthRepositoryImpl(db);
    private static final String BASE32_SECRET = "IKH7F4ZCOB7T6X2I";


    @Test
    public void testGetSecret() {
        byte[] secret = repository.getSecret(ACCOUNT1.getAccount());
        Assert.assertArrayEquals(ACCOUNT1_2FA_SECRET_BYTES, secret);
    }

    @Test
    public void testGetSecretNotFound() {

        byte[] secret = repository.getSecret(ACCOUNT2.getAccount());
        Assert.assertNull(secret);
    }

    @Test
    public void testSaveSecret() {
        byte[] secretBytes = new Base32().decode(BASE32_SECRET);
        boolean saved = repository.saveSecret(ACCOUNT2.getAccount(), secretBytes);
        Assert.assertTrue(saved);
        byte[] actualSecretBytes = repository.getSecret(ACCOUNT2.getAccount());
        Assert.assertArrayEquals(secretBytes, actualSecretBytes);

    }

    @Test
    public void testSaveSecretAlreadyExist() {
        byte[] secretBytes = new Base32().decode(BASE32_SECRET);
        boolean saved = repository.saveSecret(ACCOUNT1.getAccount(), secretBytes);
        Assert.assertFalse(saved);
    }

    @Test
    public void testDelete() {
        boolean deleted = repository.delete(ACCOUNT1.getAccount());
        Assert.assertTrue(deleted);
    }

    @Test
    public void testDeleteNothingToDelete() {
        boolean deleted = repository.delete(ACCOUNT2.getAccount());
        Assert.assertFalse(deleted);
    }
}
