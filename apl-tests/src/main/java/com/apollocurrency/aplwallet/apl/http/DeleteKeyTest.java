/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import java.io.IOException;
import java.security.GeneralSecurityException;

import com.apollocurrency.aplwallet.apl.BasicAccount;
import com.apollocurrency.aplwallet.apl.GeneratedAccount;
import com.apollocurrency.aplwallet.apl.KeyStore;
import com.apollocurrency.aplwallet.apl.TestConstants;
import com.apollocurrency.aplwallet.apl.TwoFactorAuthService;
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import dto.TwoFactorAuthAccountDetails;
import net.javacrumbs.jsonunit.fluent.JsonFluentAssert;
import org.junit.Assert;
import org.junit.Test;
import util.TestUtil;

public class DeleteKeyTest extends DeleteGeneratedAccountsTest {
    @Test
    public void testDeleteKey() throws IOException {
        GeneratedAccount account = generateAccount();
        BasicAccount basicAccount = nodeClient.deleteKey(TestConstants.TEST_LOCALHOST, account.getId(), account.getPassphrase(), 0);
        Assert.assertEquals(account.getAccountRS(), basicAccount.getAccountRS());
    }
    @Test
    public void testDeleteKeyNotFound() throws IOException {
        String json = nodeClient.deleteKeyJson(TestConstants.TEST_LOCALHOST, 100, PASSPHRASE, 0);
        JsonFluentAssert.assertThatJson(json)
                .isPresent()
                .node("errorDescription")
                .isPresent()
                .matches(TestUtil.createStringMatcher(KeyStore.Status.NOT_FOUND.message));
    }

    @Test
    public void testDeleteKeyWrongPassphrase() throws IOException {
        GeneratedAccount account = generateAccount();
        String json = nodeClient.deleteKeyJson(TestConstants.TEST_LOCALHOST, account.getId(), "another passphrase", 0);
        JsonFluentAssert.assertThatJson(json)
                .isPresent()
                .node("errorDescription")
                .isPresent()
                .matches(TestUtil.createStringMatcher(KeyStore.Status.DECRYPTION_ERROR.message));
    }

    @Test
    public void testDeleteKey2FA() throws IOException, GeneralSecurityException {
        GeneratedAccount account = generateAccount();
        long code = enable2FA(account);
        BasicAccount basicAccount = nodeClient.deleteKey(TestConstants.TEST_LOCALHOST, account.getId(), account.getPassphrase(), code);
        Assert.assertEquals(account.getAccountRS(), basicAccount.getAccountRS());
    }
    @Test
    public void testDeleteKey2FAIncorrectCode() throws IOException, GeneralSecurityException {
        GeneratedAccount account = generateAccount();
        long code = enable2FA(account);
        String json = nodeClient.deleteKeyJson(TestConstants.TEST_LOCALHOST, account.getId(), account.getPassphrase(), -code);
        JsonFluentAssert.assertThatJson(json)
                .isPresent()
                .node("errorDescription")
                .isPresent()
                .matches(TestUtil.createStringMatcher(TwoFactorAuthService.Status2FA.INCORRECT_CODE));
    }

    private long enable2FA(GeneratedAccount account) throws GeneralSecurityException, IOException {

        TwoFactorAuthAccountDetails twoFactorAuthAccountDetails = nodeClient.enable2FA(TestConstants.TEST_LOCALHOST, account.getAccountRS(), account.getPassphrase());
        long code = TimeBasedOneTimePasswordUtil.generateCurrentNumber(twoFactorAuthAccountDetails.getDetails().getSecret());
        nodeClient.confirm2FA(TestConstants.TEST_LOCALHOST, new BasicAccount(account.getAccountRS()), account.getPassphrase(),
                code);
        return code;
    }
}
