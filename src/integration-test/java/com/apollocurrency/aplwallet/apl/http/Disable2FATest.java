/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import static com.apollocurrency.aplwallet.apl.TestConstants.TEST_LOCALHOST;
import static com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData.ACCOUNT1;

import com.apollocurrency.aplwallet.apl.BasicAccount;
import com.apollocurrency.aplwallet.apl.GeneratedAccount;
import com.apollocurrency.aplwallet.apl.SecretBytesDetails;
import com.apollocurrency.aplwallet.apl.TwoFactorAuthService;
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

        long code = TimeBasedOneTimePasswordUtil.generateCurrentNumber(details.getDetails().getSecret());
        nodeClient.confirm2FA(TEST_LOCALHOST, details.getAccount(), PASSPHRASE,
                code);
        Account2FA account = nodeClient.disable2FA(TEST_LOCALHOST, Convert.rsAccount(generatedAccount.getId()), PASSPHRASE,
                code);

        Assert.assertEquals(details.getAccount().getAccountRS(), account.getAccountRS());
        Assert.assertEquals(generatedAccount.getId(), account.getId());
        Assert.assertEquals(TwoFactorAuthService.Status2FA.OK, account.getStatus2FA());
    }
    @Test
    public void testDisable2FANoAccount() throws IOException, GeneralSecurityException {
        String json = nodeClient.disable2FAJson(TEST_LOCALHOST, ACCOUNT1.getAccountRS(), PASSPHRASE, 100L);
        JsonFluentAssert.assertThatJson(json)
                .node("error")
                .isPresent()
                .isEqualTo(SecretBytesDetails.ExtractStatus.NOT_FOUND);
    }

    @Test
    public void testDisable2FAIncorrectPassphrase() throws IOException, GeneralSecurityException {
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
    public void testDisable2FAIncorrectCode() throws IOException, GeneralSecurityException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TEST_LOCALHOST, PASSPHRASE);
        generatedAccounts.add(Convert.rsAccount(generatedAccount.getId()));

        TwoFactorAuthAccountDetails twoFactorAuthAccountDetails = nodeClient.enable2FA(TEST_LOCALHOST, String.valueOf(generatedAccount.getId()), PASSPHRASE);
        nodeClient.confirm2FA(TEST_LOCALHOST, new BasicAccount(generatedAccount.getAccountRS()), PASSPHRASE,
                TimeBasedOneTimePasswordUtil.generateCurrentNumber(twoFactorAuthAccountDetails.getDetails().getSecret()));

        Account2FA account = nodeClient.disable2FA(TEST_LOCALHOST, Convert.rsAccount(generatedAccount.getId()), PASSPHRASE, 100);
        Assert.assertEquals(TwoFactorAuthService.Status2FA.INCORRECT_CODE, account.getStatus2FA());
    }

}
