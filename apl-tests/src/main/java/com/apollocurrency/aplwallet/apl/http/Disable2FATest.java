/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import static com.apollocurrency.aplwallet.apl.TestConstants.TEST_LOCALHOST;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT1;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.INVALID_CODE;

import java.io.IOException;
import java.security.GeneralSecurityException;

import com.apollocurrency.aplwallet.apl.BasicAccount;
import com.apollocurrency.aplwallet.apl.GeneratedAccount;
import com.apollocurrency.aplwallet.apl.KeyStore;
import com.apollocurrency.aplwallet.apl.PassphraseGenerator;
import com.apollocurrency.aplwallet.apl.PassphraseGeneratorImpl;
import com.apollocurrency.aplwallet.apl.TwoFactorAuthService;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import dto.Account2FA;
import dto.TwoFactorAuthAccountDetails;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import util.TestUtil;

public class Disable2FATest extends DeleteGeneratedAccountsTest {
    private static PassphraseGenerator generator = new PassphraseGeneratorImpl();
    @Before
    public void setUp() throws Exception {
        runner.disableReloading();
    }

    @Test
    public void testDisable2FAVaultWallet() throws IOException, GeneralSecurityException {
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
    public void testDisable2FANoAccountVaultWallet() throws IOException, GeneralSecurityException {
        String json = nodeClient.disable2FAJson(TEST_LOCALHOST, ACCOUNT1.getAccountRS(), PASSPHRASE, 100L);
        TestUtil.verifyJsonAccountId(json, ACCOUNT1.getId());
        TestUtil.verifyErrorDescriptionJsonNodeContains(json, KeyStore.Status.NOT_FOUND.message);
    }

    @Test
    public void testDisable2FAIncorrectPassphraseVaultWallet() throws IOException, GeneralSecurityException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TEST_LOCALHOST, PASSPHRASE);
        generatedAccounts.add(Convert.rsAccount(generatedAccount.getId()));

        TwoFactorAuthAccountDetails details = nodeClient.enable2FA(TEST_LOCALHOST, String.valueOf(generatedAccount.getId()), PASSPHRASE);
        nodeClient.confirm2FA(TEST_LOCALHOST, new BasicAccount(generatedAccount.getAccountRS()), PASSPHRASE,
                TimeBasedOneTimePasswordUtil.generateCurrentNumber(details.getDetails().getSecret()));

        String json = nodeClient.disable2FAJson(TEST_LOCALHOST, Convert.rsAccount(generatedAccount.getId()), PASSPHRASE + "1",
                TimeBasedOneTimePasswordUtil.generateCurrentNumber(details.getDetails().getSecret()));
        TestUtil.verifyErrorDescriptionJsonNodeContains(json, KeyStore.Status.DECRYPTION_ERROR.message);
        TestUtil.verifyJsonAccountId(json, generatedAccount.getId());
    }



    @Test
    public void testDisable2FAIncorrectCodeVaultWallet() throws IOException, GeneralSecurityException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TEST_LOCALHOST, PASSPHRASE);
        generatedAccounts.add(Convert.rsAccount(generatedAccount.getId()));

        TwoFactorAuthAccountDetails twoFactorAuthAccountDetails = nodeClient.enable2FA(TEST_LOCALHOST, String.valueOf(generatedAccount.getId()), PASSPHRASE);
        nodeClient.confirm2FA(TEST_LOCALHOST, new BasicAccount(generatedAccount.getAccountRS()), PASSPHRASE,
                TimeBasedOneTimePasswordUtil.generateCurrentNumber(twoFactorAuthAccountDetails.getDetails().getSecret()));

        String json = nodeClient.disable2FAJson(TEST_LOCALHOST, Convert.rsAccount(generatedAccount.getId()), PASSPHRASE, 100);
        TestUtil.verifyErrorDescriptionJsonNodeContains(json, TwoFactorAuthService.Status2FA.INCORRECT_CODE);
        TestUtil.verifyJsonAccountId(json, generatedAccount.getId());
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

        String json = nodeClient.disable2FAJson(TEST_LOCALHOST, secretPhrase, INVALID_CODE);
        TestUtil.verifyErrorDescriptionJsonNodeContains(json, TwoFactorAuthService.Status2FA.INCORRECT_CODE);
        TestUtil.verifyJsonAccountId(json, id);
    }
    @Test
    public void testDisable2FANotEnabledOnlineWallet() throws IOException, GeneralSecurityException {
        String secretPhrase = generator.generate();


        long id = Convert.getId(Crypto.getPublicKey(secretPhrase));
        String json = nodeClient.disable2FAJson(TEST_LOCALHOST, secretPhrase, INVALID_CODE);
        TestUtil.verifyErrorDescriptionJsonNodeContains(json, TwoFactorAuthService.Status2FA.NOT_ENABLED);
        TestUtil.verifyJsonAccountId(json, id);
    }
    @Test
    public void testDisable2FANotConfirmedAccountOnlineWallet() throws IOException, GeneralSecurityException {
        String secretPhrase = generator.generate();
        nodeClient.enable2FA(TEST_LOCALHOST, secretPhrase);
        long id = Convert.getId(Crypto.getPublicKey(secretPhrase));
        String json = nodeClient.disable2FAJson(TEST_LOCALHOST, secretPhrase, INVALID_CODE);
        TestUtil.verifyErrorDescriptionJsonNodeContains(json, TwoFactorAuthService.Status2FA.NOT_CONFIRMED);
        TestUtil.verifyJsonAccountId(json, id);
    }



}
