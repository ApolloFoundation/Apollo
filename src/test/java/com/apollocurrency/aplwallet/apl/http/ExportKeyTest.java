/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import dto.AccountKey;
import com.apollocurrency.aplwallet.apl.GeneratedAccount;
import com.apollocurrency.aplwallet.apl.TestData;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ExportKeyTest extends DeleteGeneratedAccountsTest {

    @Test
    public void testExportKey() throws IOException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TestData.TEST_LOCALHOST, PASSPHRASE);
        generatedAccounts.add(Convert.rsAccount(generatedAccount.getId()));
        AccountKey accountKey = nodeClient.exportKey(TestData.TEST_LOCALHOST, PASSPHRASE, Convert.rsAccount(generatedAccount.getId()));
        Assert.assertEquals(PASSPHRASE, generatedAccount.getPassphrase());
        Assert.assertEquals(Convert.rsAccount(generatedAccount.getId()), accountKey.getAccount());
    }

    @Test(expected = RuntimeException.class)
    public void testExportKeyInvalidAccount() throws IOException {
        nodeClient.exportKey(TestData.TEST_LOCALHOST, PASSPHRASE, Convert.rsAccount(1));
    }
    @Test(expected = RuntimeException.class)
    public void testExportKeyInvalidPassphrase() throws IOException {
        GeneratedAccount generatedAccount = nodeClient.generateAccount(TestData.TEST_LOCALHOST, PASSPHRASE);
        generatedAccounts.add(Convert.rsAccount(generatedAccount.getId()));
        nodeClient.exportKey(TestData.TEST_LOCALHOST, "anotherpassphrase", Convert.rsAccount(generatedAccount.getId()));
    }
}
