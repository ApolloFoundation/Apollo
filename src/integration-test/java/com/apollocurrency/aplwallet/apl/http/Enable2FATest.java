/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import static com.apollocurrency.aplwallet.apl.TestConstants.TEST_LOCALHOST;

import java.io.IOException;

import com.apollocurrency.aplwallet.apl.GeneratedAccount;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData;
import com.apollocurrency.aplwallet.apl.util.Convert;
import dto.TwoFactorAuthAccountDetails;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import util.TwoFactorAuthUtil;

public class Enable2FATest extends DeleteGeneratedAccountsTest {
    @Before
    public void setUp() {
        runner.disableReloading();
    }
    private static final String SECRET_PHRASE = "Test_Secret_Phrase";
    @Test
    public void testEnable2FAVaultWallet() throws IOException {

        GeneratedAccount generatedAccount = nodeClient.generateAccount(TEST_LOCALHOST, PASSPHRASE);
        generatedAccounts.add(Convert.rsAccount(generatedAccount.getId()));
        TwoFactorAuthAccountDetails details = nodeClient.enable2FA(TEST_LOCALHOST, Convert.rsAccount(generatedAccount.getId()), PASSPHRASE);
        Assert.assertEquals(generatedAccount.getId(), details.getAccount().getId());
        TwoFactorAuthUtil.verifySecretCode(details.getDetails(), Convert.rsAccount(generatedAccount.getId()));
    }
    @Test
    public void testEnable2FAOnlineWallet() throws IOException {

        TwoFactorAuthAccountDetails details = nodeClient.enable2FA(TEST_LOCALHOST, SECRET_PHRASE);
        long accountId = Convert.getId(Crypto.getPublicKey(SECRET_PHRASE));
        Assert.assertEquals(accountId, details.getAccount().getId());
        TwoFactorAuthUtil.verifySecretCode(details.getDetails(), Convert.rsAccount(accountId));
    }

    @Test(expected = RuntimeException.class)
    public void testEnable2FANoSuchAccountVaultWallet() throws IOException {
       nodeClient.enable2FA(TEST_LOCALHOST, TwoFactorAuthTestData.ACCOUNT1.getAccountRS(), PASSPHRASE);
    }
    @Test(expected = RuntimeException.class)
    public void testEnable2FAInvalidPassphraseVaultWallet() throws IOException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TEST_LOCALHOST, PASSPHRASE);
        generatedAccounts.add(Convert.rsAccount(generatedAccount.getId()));
        nodeClient.enable2FA(TEST_LOCALHOST, Convert.rsAccount(generatedAccount.getId()), "anotherpass");
    }
}
