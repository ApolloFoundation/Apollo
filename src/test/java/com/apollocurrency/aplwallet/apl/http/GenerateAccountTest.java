/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.GeneratedAccount;
import com.apollocurrency.aplwallet.apl.TestConstants;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class GenerateAccountTest extends DeleteGeneratedAccountsTest {
    @Test
    public void testGenerateAccount() throws IOException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TestConstants.TEST_LOCALHOST, PASSPHRASE);
        generatedAccounts.add(Convert.rsAccount(generatedAccount.getId()));
        Assert.assertEquals(PASSPHRASE, generatedAccount.getPassphrase());
    }

    @Test
    public void testGenerateAccountWithoutPassphrase() throws IOException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TestConstants.TEST_LOCALHOST, null);
        generatedAccounts.add(Convert.rsAccount(generatedAccount.getId()));
        Assert.assertNotNull(generatedAccount.getPublicKey());
        Assert.assertNotEquals(0, generatedAccount.getId());
        Assert.assertNotEquals(PASSPHRASE, generatedAccount.getPassphrase());
    }
}
