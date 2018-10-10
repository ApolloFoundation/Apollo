/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import dto.AccountWithKey;
import com.apollocurrency.aplwallet.apl.GeneratedAccount;
import com.apollocurrency.aplwallet.apl.TestConstants;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ExportKeyTest extends DeleteGeneratedAccountsTest {

    @Test
    public void testExportKey() throws IOException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TestConstants.TEST_LOCALHOST, PASSPHRASE);
        generatedAccounts.add(Convert.rsAccount(generatedAccount.getId()));
        AccountWithKey accountWithKey = nodeClient.exportKey(TestConstants.TEST_LOCALHOST, PASSPHRASE, Convert.rsAccount(generatedAccount.getId()));
        Assert.assertEquals(PASSPHRASE, generatedAccount.getPassphrase());
        Assert.assertEquals(generatedAccount.getId(), accountWithKey.getId());
    }

    @Test(expected = RuntimeException.class)
    public void testExportKeyInvalidAccount() throws IOException {
        nodeClient.exportKey(TestConstants.TEST_LOCALHOST, PASSPHRASE, Convert.rsAccount(1));
    }
    @Test(expected = RuntimeException.class)
    public void testExportKeyInvalidPassphrase() throws IOException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TestConstants.TEST_LOCALHOST, PASSPHRASE);
        generatedAccounts.add(Convert.rsAccount(generatedAccount.getId()));
        nodeClient.exportKey(TestConstants.TEST_LOCALHOST, "anotherpassphrase", Convert.rsAccount(generatedAccount.getId()));
    }
}
