/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.inttest.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.apollocurrency.aplwallet.apl.GeneratedAccount;
import com.apollocurrency.aplwallet.apl.inttest.core.TestConstants;
import org.junit.After;
import com.apollocurrency.aplwallet.apl.inttest.util.TestUtil;

public abstract class DeleteGeneratedAccountsTest extends APITest {
    //list hold all generated during tests accounts (accountRS representation)
//    to delete it from keystore after test
    protected List<String> generatedAccounts = new ArrayList<>();
    @After
    public void tearDown() throws Exception {
        TestUtil.deleteInKeystore(generatedAccounts);
    }

    protected GeneratedAccount generateAccount() throws IOException {
        GeneratedAccount account = nodeClient.generateAccount(TestConstants.TEST_LOCALHOST, PASSPHRASE);
        generatedAccounts.add(account.getAccountRS());
        return account;
    }

}
