/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.db;

import com.apollocurrency.aplwallet.apl.AlreadyExistsException;
import com.apollocurrency.aplwallet.apl.DbIntegrationTest;
import com.apollocurrency.aplwallet.apl.NotFoundException;
import org.apache.commons.codec.binary.Base32;
import org.junit.Assert;
import org.junit.Test;

import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.*;


public class TwoFactorAuthRepositoryIntegrationTest extends DbIntegrationTest {
    private TwoFactorAuthRepository repository = new TwoFactorAuthRepositoryImpl(db);
    private static final String BASE32_SECRET = "IKH7F4ZCOB7T6X2I";
    private static final Base32 BASE32 = new Base32();


    @Test
    public void testGetSecret() {
        byte[] secret = repository.getSecret(ACCOUNT1.getAccount());
        Assert.assertArrayEquals(ACCOUNT1_2FA_SECRET_BYTES, secret);
    }

    @Test(expected = NotFoundException.class)
    public void testGetSecretNotFount() {
        repository.getSecret(ACCOUNT2.getAccount());
    }

    @Test
    public void testSaveSecret() {
        byte[] secretBytes = new Base32().decode(BASE32_SECRET);
        repository.saveSecret(ACCOUNT2.getAccount(), secretBytes);

        byte[] actualSecretBytes = repository.getSecret(ACCOUNT2.getAccount());
        Assert.assertArrayEquals(secretBytes, actualSecretBytes);

    }

    @Test(expected = AlreadyExistsException.class)
    public void testSaveSecretAlreadyExist() {
        byte[] secretBytes = new Base32().decode(BASE32_SECRET);
        repository.saveSecret(ACCOUNT1.getAccount(), secretBytes);
    }

    @Test
    public void testDelete() {
        // should return existing secret (should be exception if secret is not exist)
        repository.getSecret(ACCOUNT1.getAccount());

        // remove current secret
        repository.delete(ACCOUNT1.getAccount());
        ;
        byte[] secretBytes = BASE32.decode(BASE32_SECRET);

        // save old secret (should be exception if secret was not deleted)
        repository.saveSecret(ACCOUNT1.getAccount(), secretBytes);

        // get saved secret and compare
        byte[] actualSecret = repository.getSecret(ACCOUNT1.getAccount());
        Assert.assertArrayEquals(secretBytes, actualSecret);
    }

    @Test(expected = NotFoundException.class)
    public void testDeleteNotFound() {
        repository.delete(ACCOUNT2.getAccount());
    }
}
