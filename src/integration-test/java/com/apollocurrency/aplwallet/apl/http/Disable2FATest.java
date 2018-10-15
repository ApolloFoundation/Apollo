/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.*;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import dto.Account2FA;
import dto.TwoFactorAuthAccountDetails;
import net.javacrumbs.jsonunit.fluent.JsonFluentAssert;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static com.apollocurrency.aplwallet.apl.TestConstants.TEST_LOCALHOST;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT1;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.INVALID_CODE;

public class Disable2FATest extends DeleteGeneratedAccountsTest {
    private static PassphraseGenerator generator = new PassphraseGeneratorImpl();
    @Before
    public void setUp() throws Exception {
        runner.disableReloading();
    }

    @Test
    public void testDisable2FASureWallet() throws IOException, GeneralSecurityException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TEST_LOCALHOST, PASSPHRASE);
        generatedAccounts.add(Convert.rsAccount(generatedAccount.getId()));

        TwoFactorAuthAccountDetails details = nodeClient.enable2FA(TEST_LOCALHOST, Convert.rsAccount(generatedAccount.getId()), PASSPHRASE);

        long code = TimeBasedOneTimePasswordUtil.generateCurrentNumber(details.getDetails().getSecret());
        nodeClient.confirm2FA(TEST_LOCALHOST, details.getAccount(), PASSPHRASE,
                code);
        Account2FA account = nodeClient.disable2FA(TEST_LOCALHOST, Convert.rsAccount(generatedAccount.getId()), PASSPHRASE, null,
                code);

        Assert.assertEquals(details.getAccount().getAccountRS(), account.getAccountRS());
        Assert.assertEquals(generatedAccount.getId(), account.getId());
        Assert.assertEquals(TwoFactorAuthService.Status2FA.OK, account.getStatus2FA());
    }
    @Test
    public void testDisable2FANoAccountSureWallet() throws IOException, GeneralSecurityException {
        String json = nodeClient.disable2FAJson(TEST_LOCALHOST, ACCOUNT1.getAccountRS(), PASSPHRASE, 100L);
        JsonFluentAssert.assertThatJson(json)
                .node("error")
                .isPresent()
                .isEqualTo(SecretBytesDetails.ExtractStatus.NOT_FOUND);
    }

    @Test
    public void testDisable2FAIncorrectPassphraseSureWallet() throws IOException, GeneralSecurityException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TEST_LOCALHOST, PASSPHRASE);
        generatedAccounts.add(Convert.rsAccount(generatedAccount.getId()));

        TwoFactorAuthAccountDetails details = nodeClient.enable2FA(TEST_LOCALHOST, String.valueOf(generatedAccount.getId()), PASSPHRASE);
        nodeClient.confirm2FA(TEST_LOCALHOST, new BasicAccount(generatedAccount.getAccountRS()), PASSPHRASE,
                TimeBasedOneTimePasswordUtil.generateCurrentNumber(details.getDetails().getSecret()));

        String disable2FAJson = nodeClient.disable2FAJson(TEST_LOCALHOST, Convert.rsAccount(generatedAccount.getId()), PASSPHRASE + "1",
                TimeBasedOneTimePasswordUtil.generateCurrentNumber(details.getDetails().getSecret()));

        JsonFluentAssert.assertThatJson(disable2FAJson)
                .node("error")
                .isPresent()
                .isEqualTo(SecretBytesDetails.ExtractStatus.DECRYPTION_ERROR);
    }

    @Test
    public void testDisable2FAIncorrectCodeSureWallet() throws IOException, GeneralSecurityException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TEST_LOCALHOST, PASSPHRASE);
        generatedAccounts.add(Convert.rsAccount(generatedAccount.getId()));

        TwoFactorAuthAccountDetails twoFactorAuthAccountDetails = nodeClient.enable2FA(TEST_LOCALHOST, String.valueOf(generatedAccount.getId()), PASSPHRASE);
        nodeClient.confirm2FA(TEST_LOCALHOST, new BasicAccount(generatedAccount.getAccountRS()), PASSPHRASE,
                TimeBasedOneTimePasswordUtil.generateCurrentNumber(twoFactorAuthAccountDetails.getDetails().getSecret()));

        Account2FA account = nodeClient.disable2FA(TEST_LOCALHOST, Convert.rsAccount(generatedAccount.getId()), PASSPHRASE, null, 100);
        Assert.assertEquals(TwoFactorAuthService.Status2FA.INCORRECT_CODE, account.getStatus2FA());
    }
    @Test
    public void testDisable2FAOnlineWallet() throws IOException, GeneralSecurityException {
        String secretPhrase = generator.generate();
        long id = Convert.getId(Crypto.getPublicKey(secretPhrase));

        TwoFactorAuthAccountDetails details = nodeClient.enable2FA(TEST_LOCALHOST, secretPhrase);

        long code = TimeBasedOneTimePasswordUtil.generateCurrentNumber(details.getDetails().getSecret());
        nodeClient.confirm2FA(TEST_LOCALHOST, secretPhrase, code);

        Account2FA account = nodeClient.disable2FA(TEST_LOCALHOST, null, null, secretPhrase, code);

        Assert.assertEquals(details.getAccount().getAccountRS(), account.getAccountRS());
        Assert.assertEquals(id, account.getId());
        Assert.assertEquals(TwoFactorAuthService.Status2FA.OK, account.getStatus2FA());
    }

    @Test
    public void testDisable2FAIncorrectCodeOnlineWallet() throws IOException, GeneralSecurityException {
        String secretPhrase = generator.generate();
        long id = Convert.getId(Crypto.getPublicKey(secretPhrase));

        TwoFactorAuthAccountDetails details = nodeClient.enable2FA(TEST_LOCALHOST, secretPhrase);

        long code = TimeBasedOneTimePasswordUtil.generateCurrentNumber(details.getDetails().getSecret());
        nodeClient.confirm2FA(TEST_LOCALHOST, secretPhrase, code);

        Account2FA account = nodeClient.disable2FA(TEST_LOCALHOST, null, null, secretPhrase, INVALID_CODE);

        Assert.assertEquals(details.getAccount().getAccountRS(), account.getAccountRS());
        Assert.assertEquals(id, account.getId());
        Assert.assertEquals(TwoFactorAuthService.Status2FA.INCORRECT_CODE, account.getStatus2FA());
    }
    @Test
    public void testDisable2FANotEnabledOnlineWallet() throws IOException, GeneralSecurityException {
        String secretPhrase = generator.generate();
        Account2FA account2FA = nodeClient.disable2FA(TEST_LOCALHOST, null, null, secretPhrase, INVALID_CODE);
        Assert.assertEquals(Convert.getId(Crypto.getPublicKey(secretPhrase)), account2FA.getId());
        Assert.assertEquals(TwoFactorAuthService.Status2FA.NOT_ENABLED, account2FA.getStatus2FA());
    }
    @Test
    public void testDisable2FANotConfirmedAccountOnlineWallet() throws IOException, GeneralSecurityException {
        String secretPhrase = generator.generate();
        TwoFactorAuthAccountDetails details = nodeClient.enable2FA(TEST_LOCALHOST, secretPhrase);
        Account2FA account2FA = nodeClient.disable2FA(TEST_LOCALHOST, null, null, secretPhrase, INVALID_CODE);
        Assert.assertEquals(Convert.getId(Crypto.getPublicKey(secretPhrase)), account2FA.getId());
        Assert.assertEquals(account2FA.getId(), details.getAccount().getId());
        Assert.assertEquals(TwoFactorAuthService.Status2FA.NOT_CONFIRMED, account2FA.getStatus2FA());
    }



}
