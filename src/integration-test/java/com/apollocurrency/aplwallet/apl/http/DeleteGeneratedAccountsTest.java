/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import org.junit.After;
import util.TestUtil;

import java.util.ArrayList;
import java.util.List;

public abstract class DeleteGeneratedAccountsTest extends APITest {
    //list hold all generated during tests accounts (accountRS representation)
//    to delete it from keystore after test
    protected List<String> generatedAccounts = new ArrayList<>();
    @After
    public void tearDown() throws Exception {
        TestUtil.deleteInKeystore(generatedAccounts);
    }
}
