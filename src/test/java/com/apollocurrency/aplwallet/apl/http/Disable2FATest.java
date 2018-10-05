/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.GeneratedAccount;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import dto.Account;
import dto.TwoFactorAuthAccountDetails;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static com.apollocurrency.aplwallet.apl.TestConstants.TEST_LOCALHOST;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT1;

public class Disable2FATest extends DeleteGeneratedAccountsTest {
    @Before
    public void setUp() throws Exception {
        runner.disableReloading();
    }

    @Test
    public void testDisable2FA() throws IOException, GeneralSecurityException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TEST_LOCALHOST, PASSPHRASE);
        generatedAccounts.add(Convert.rsAccount(generatedAccount.getId()));

        TwoFactorAuthAccountDetails details = nodeClient.enable2FA(TEST_LOCALHOST, Convert.rsAccount(generatedAccount.getId()), PASSPHRASE);
        Account account = nodeClient.disable2FA(TEST_LOCALHOST, Convert.rsAccount(generatedAccount.getId()), PASSPHRASE,
                TimeBasedOneTimePasswordUtil.generateCurrentNumber(details.getDetails().getSecret()));
        Assert.assertEquals(details.getAccount().getAccountRS(), account.getAccountRS());
        Assert.assertEquals(generatedAccount.getId(), account.getId());
    }
    @Test(expected = RuntimeException.class)
    public void testDisable2FAAccount() throws IOException, GeneralSecurityException {
        nodeClient.disable2FA(TEST_LOCALHOST, ACCOUNT1.getAccountRS(), PASSPHRASE, 100L);
    }

    @Test(expected = RuntimeException.class)
    public void testDisable2FAIncorrectPassphrase() throws IOException, GeneralSecurityException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TEST_LOCALHOST, PASSPHRASE);
        generatedAccounts.add(Convert.rsAccount(generatedAccount.getId()));

        TwoFactorAuthAccountDetails details = nodeClient.enable2FA(TEST_LOCALHOST, String.valueOf(generatedAccount.getId()), PASSPHRASE);

       nodeClient.disable2FA(TEST_LOCALHOST, Convert.rsAccount(generatedAccount.getId()), PASSPHRASE+"1",
                TimeBasedOneTimePasswordUtil.generateCurrentNumber(details.getDetails().getSecret()));
    }

    @Test(expected = RuntimeException.class)
    public void testDisable2FAIncorrectCode() throws IOException, GeneralSecurityException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TEST_LOCALHOST, PASSPHRASE);
        generatedAccounts.add(Convert.rsAccount(generatedAccount.getId()));

        nodeClient.enable2FA(TEST_LOCALHOST, String.valueOf(generatedAccount.getId()), PASSPHRASE);

        nodeClient.disable2FA(TEST_LOCALHOST, Convert.rsAccount(generatedAccount.getId()), PASSPHRASE, 100);
    }

}
