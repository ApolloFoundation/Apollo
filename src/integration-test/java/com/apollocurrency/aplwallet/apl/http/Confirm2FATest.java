/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;


import com.apollocurrency.aplwallet.apl.GeneratedAccount;
import com.apollocurrency.aplwallet.apl.TestConstants;
import com.apollocurrency.aplwallet.apl.TwoFactorAuthService;
import com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import dto.Account2FA;
import dto.TwoFactorAuthAccountDetails;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class Confirm2FATest extends DeleteGeneratedAccountsTest {
    @Test
    public void testConfirm2FA() throws IOException, GeneralSecurityException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TestConstants.TEST_LOCALHOST, PASSPHRASE);
        generatedAccounts.add(Convert.rsAccount(generatedAccount.getId()));
        TwoFactorAuthAccountDetails details = nodeClient.enable2FA(TestConstants.TEST_LOCALHOST, Convert.rsAccount(generatedAccount.getId()),
                PASSPHRASE);
        Account2FA account = nodeClient.confirm2FA(TestConstants.TEST_LOCALHOST, generatedAccount, PASSPHRASE,
                TimeBasedOneTimePasswordUtil.generateCurrentNumber(details.getDetails().getSecret()));
        Assert.assertEquals(details.getAccount().getAccountRS(), account.getAccountRS());
        Assert.assertEquals(generatedAccount.getId(), account.getId());
        Assert.assertEquals(TwoFactorAuthService.Status2FA.OK, account.getStatus2FA());
    }

    @Test(expected = MismatchedInputException.class)
    public void testConfirm2FANotConfirmedWhenWrongPassphrase() throws IOException, GeneralSecurityException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TestConstants.TEST_LOCALHOST, PASSPHRASE);
        generatedAccounts.add(Convert.rsAccount(generatedAccount.getId()));
        TwoFactorAuthAccountDetails details = nodeClient.enable2FA(TestConstants.TEST_LOCALHOST, Convert.rsAccount(generatedAccount.getId()),
                PASSPHRASE);
        nodeClient.confirm2FA(TestConstants.TEST_LOCALHOST, generatedAccount, TwoFactorAuthTestData.INVALID_PASSPHRASE,
                TimeBasedOneTimePasswordUtil.generateCurrentNumber(details.getDetails().getSecret()));
    }
    @Test
    public void testConfirm2FANotConfirmedWhenInvalidCode() throws IOException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TestConstants.TEST_LOCALHOST, PASSPHRASE);
        generatedAccounts.add(Convert.rsAccount(generatedAccount.getId()));
        nodeClient.enable2FA(TestConstants.TEST_LOCALHOST, Convert.rsAccount(generatedAccount.getId()),
                PASSPHRASE);
        Account2FA account2FA = nodeClient.confirm2FA(TestConstants.TEST_LOCALHOST, generatedAccount, PASSPHRASE, TwoFactorAuthTestData.INVALID_CODE);
        Assert.assertEquals(generatedAccount.getAccountRS(), account2FA.getAccountRS());
        Assert.assertEquals(TwoFactorAuthService.Status2FA.INCORRECT_CODE, account2FA.getStatus2FA());
    }
    @Test
    public void testConfirm2FANotConfirmedWhenNotEnabled() throws IOException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TestConstants.TEST_LOCALHOST, PASSPHRASE);
        generatedAccounts.add(Convert.rsAccount(generatedAccount.getId()));
        Account2FA account2FA = nodeClient.confirm2FA(TestConstants.TEST_LOCALHOST, generatedAccount, PASSPHRASE, TwoFactorAuthTestData.INVALID_CODE);
        Assert.assertEquals(generatedAccount.getAccountRS(), account2FA.getAccountRS());
        Assert.assertEquals(TwoFactorAuthService.Status2FA.NOT_ENABLED, account2FA.getStatus2FA());
    }
    @Test
    public void testConfirm2FAAlreadyConfirmed() throws IOException, GeneralSecurityException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TestConstants.TEST_LOCALHOST, PASSPHRASE);
        generatedAccounts.add(Convert.rsAccount(generatedAccount.getId()));
        TwoFactorAuthAccountDetails details = nodeClient.enable2FA(TestConstants.TEST_LOCALHOST, Convert.rsAccount(generatedAccount.getId()),
                PASSPHRASE);
        Account2FA account = nodeClient.confirm2FA(TestConstants.TEST_LOCALHOST, generatedAccount, PASSPHRASE,
                TimeBasedOneTimePasswordUtil.generateCurrentNumber(details.getDetails().getSecret()));
        Assert.assertEquals(details.getAccount().getAccountRS(), account.getAccountRS());
        Assert.assertEquals(generatedAccount.getId(), account.getId());
        Assert.assertEquals(TwoFactorAuthService.Status2FA.OK, account.getStatus2FA());
        Account2FA account2FA = nodeClient.confirm2FA(TestConstants.TEST_LOCALHOST, generatedAccount, PASSPHRASE,
                TimeBasedOneTimePasswordUtil.generateCurrentNumber(details.getDetails().getSecret()));
        Assert.assertEquals(TwoFactorAuthService.Status2FA.ALREADY_CONFIRMED, account2FA.getStatus2FA());
        Assert.assertEquals(details.getAccount().getAccountRS(), account2FA.getAccountRS());
        Assert.assertEquals(generatedAccount.getId(), account2FA.getId());
    }
}
