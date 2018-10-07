/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.GeneratedAccount;
import com.apollocurrency.aplwallet.apl.data.TwoFactorAuthTestData;
import com.apollocurrency.aplwallet.apl.util.Convert;
import dto.TwoFactorAuthAccountDetails;
import org.junit.Assert;
import org.junit.Test;
import util.TwoFactorAuthUtil;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static com.apollocurrency.aplwallet.apl.TestConstants.TEST_LOCALHOST;

public class Enable2FATest extends DeleteGeneratedAccountsTest {
    @Test
    public void testEnable2FA() throws IOException, GeneralSecurityException {

        GeneratedAccount generatedAccount = nodeClient.generateAccount(TEST_LOCALHOST, PASSPHRASE);
        generatedAccounts.add(Convert.rsAccount(generatedAccount.getId()));
        TwoFactorAuthAccountDetails details = nodeClient.enable2FA(TEST_LOCALHOST, Convert.rsAccount(generatedAccount.getId()), PASSPHRASE);
        Assert.assertEquals(generatedAccount.getId(), details.getAccount().getId());
        TwoFactorAuthUtil.verifySecretCode(details.getDetails(), Convert.rsAccount(generatedAccount.getId()));
    }
    @Test(expected = RuntimeException.class)
    public void testEnable2FANoSuchAccount() throws IOException {
       nodeClient.enable2FA(TEST_LOCALHOST, TwoFactorAuthTestData.ACCOUNT1.getAccountRS(), PASSPHRASE);
    }
    @Test(expected = RuntimeException.class)
    public void testEnable2FAInvalidPassphrase() throws IOException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TEST_LOCALHOST, PASSPHRASE);
        generatedAccounts.add(Convert.rsAccount(generatedAccount.getId()));
        nodeClient.enable2FA(TEST_LOCALHOST, Convert.rsAccount(generatedAccount.getId()), "anotherpass");
    }
}
