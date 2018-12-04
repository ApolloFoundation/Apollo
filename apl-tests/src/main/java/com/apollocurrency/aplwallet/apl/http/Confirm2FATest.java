/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;


import java.io.IOException;
import java.security.GeneralSecurityException;

import com.apollocurrency.aplwallet.apl.GeneratedAccount;
import com.apollocurrency.aplwallet.apl.PassphraseGenerator;
import com.apollocurrency.aplwallet.apl.PassphraseGeneratorImpl;
import com.apollocurrency.aplwallet.apl.TestConstants;
import com.apollocurrency.aplwallet.apl.TwoFactorAuthService;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import dto.Account2FA;
import dto.TwoFactorAuthAccountDetails;
import org.junit.Assert;
import org.junit.Test;

public class Confirm2FATest extends DeleteGeneratedAccountsTest {
    private static PassphraseGenerator generator = new PassphraseGeneratorImpl();
    @Test
    public void testConfirm2FAVaultWallet() throws IOException, GeneralSecurityException {
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
    public void testConfirm2FANotConfirmedWhenWrongPassphraseVaultWallet() throws IOException, GeneralSecurityException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TestConstants.TEST_LOCALHOST, PASSPHRASE);
        generatedAccounts.add(Convert.rsAccount(generatedAccount.getId()));
        TwoFactorAuthAccountDetails details = nodeClient.enable2FA(TestConstants.TEST_LOCALHOST, Convert.rsAccount(generatedAccount.getId()),
                PASSPHRASE);
        nodeClient.confirm2FA(TestConstants.TEST_LOCALHOST, generatedAccount, TwoFactorAuthTestData.INVALID_PASSPHRASE,
                TimeBasedOneTimePasswordUtil.generateCurrentNumber(details.getDetails().getSecret()));
    }
    @Test
    public void testConfirm2FANotConfirmedWhenInvalidCodeVaultWallet() throws IOException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TestConstants.TEST_LOCALHOST, PASSPHRASE);
        generatedAccounts.add(Convert.rsAccount(generatedAccount.getId()));
        nodeClient.enable2FA(TestConstants.TEST_LOCALHOST, Convert.rsAccount(generatedAccount.getId()),
                PASSPHRASE);
        Account2FA account2FA = nodeClient.confirm2FA(TestConstants.TEST_LOCALHOST, generatedAccount, PASSPHRASE, TwoFactorAuthTestData.INVALID_CODE);
        Assert.assertEquals(generatedAccount.getAccountRS(), account2FA.getAccountRS());
        Assert.assertEquals(TwoFactorAuthService.Status2FA.INCORRECT_CODE, account2FA.getStatus2FA());
    }
    @Test
    public void testConfirm2FANotConfirmedWhenNotEnabledVaultWallet() throws IOException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TestConstants.TEST_LOCALHOST, PASSPHRASE);
        generatedAccounts.add(Convert.rsAccount(generatedAccount.getId()));
        Account2FA account2FA = nodeClient.confirm2FA(TestConstants.TEST_LOCALHOST, generatedAccount, PASSPHRASE, TwoFactorAuthTestData.INVALID_CODE);
        Assert.assertEquals(generatedAccount.getAccountRS(), account2FA.getAccountRS());
        Assert.assertEquals(TwoFactorAuthService.Status2FA.NOT_ENABLED, account2FA.getStatus2FA());
    }
    @Test
    public void testConfirm2FAAlreadyConfirmedVaultWallet() throws IOException, GeneralSecurityException {
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

    @Test
    public void testConfirm2FAOnlineWallet() throws IOException, GeneralSecurityException {
        String secretPhrase = generator.generate();
        long id = Convert.getId(Crypto.getPublicKey(secretPhrase));
        TwoFactorAuthAccountDetails details = nodeClient.enable2FA(TestConstants.TEST_LOCALHOST, secretPhrase);
        Account2FA account = nodeClient.confirm2FA(TestConstants.TEST_LOCALHOST, secretPhrase,
                TimeBasedOneTimePasswordUtil.generateCurrentNumber(details.getDetails().getSecret()));
        Assert.assertEquals(details.getAccount().getAccountRS(), account.getAccountRS());
        Assert.assertEquals(id, account.getId());
        Assert.assertEquals(TwoFactorAuthService.Status2FA.OK, account.getStatus2FA());
    }

    @Test
    public void testConfirm2FANotConfirmedWhenNotEnabledOnlineWallet() throws IOException, GeneralSecurityException {
        String secretPhrase = generator.generate();
        long id = Convert.getId(Crypto.getPublicKey(secretPhrase));
        Account2FA account = nodeClient.confirm2FA(TestConstants.TEST_LOCALHOST, secretPhrase, TwoFactorAuthTestData.INVALID_CODE);
        Assert.assertEquals(id, account.getId());
        Assert.assertEquals(TwoFactorAuthService.Status2FA.NOT_ENABLED, account.getStatus2FA());
    }

    @Test
    public void testConfirm2FANotConfirmedWhenAlreadyConfirmedOnlineWallet() throws IOException, GeneralSecurityException {
        String secretPhrase = generator.generate();
        long id = Convert.getId(Crypto.getPublicKey(secretPhrase));
        TwoFactorAuthAccountDetails details = nodeClient.enable2FA(TestConstants.TEST_LOCALHOST, secretPhrase);
        Account2FA account = nodeClient.confirm2FA(TestConstants.TEST_LOCALHOST, secretPhrase,
                TimeBasedOneTimePasswordUtil.generateCurrentNumber(details.getDetails().getSecret()));
        Assert.assertEquals(details.getAccount().getAccountRS(), account.getAccountRS());
        Assert.assertEquals(id, account.getId());
        Assert.assertEquals(TwoFactorAuthService.Status2FA.OK, account.getStatus2FA());
        Account2FA account2FA = nodeClient.confirm2FA(TestConstants.TEST_LOCALHOST, secretPhrase,
                TimeBasedOneTimePasswordUtil.generateCurrentNumber(details.getDetails().getSecret()));
        Assert.assertEquals(details.getAccount().getAccountRS(), account2FA.getAccountRS());
        Assert.assertEquals(id, account2FA.getId());
        Assert.assertEquals(TwoFactorAuthService.Status2FA.ALREADY_CONFIRMED, account2FA.getStatus2FA());
    }

}
