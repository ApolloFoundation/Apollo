/*
 * Copyright © 2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.utils;

import com.apollocurrency.aplwallet.vault.util.FbWalletUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FbWalletUtilTest {

    @Test
    void getWalletFileVersion() {
        Integer version1 = FbWalletUtil.getWalletFileVersion(String.join(File.separator, "a2e9b9", "v1_2019-03-19_13-58-16---APL-9ES6-JTW3-JBN4" +
            "-7LLZQ"));
        Integer version0 = FbWalletUtil.getWalletFileVersion(String.join(File.separator, "a2e9b9", "v0_2019-03-19_13-58-16---APL-9ES6-JTW3-JBN4-7LLZQ"));

        assertTrue(Objects.equals(1, version1));
        assertTrue(Objects.equals(0, version0));
    }

    @Test
    void getWalletFileVersionWhenFileNameNotValid() {
        Integer version1 = FbWalletUtil.getWalletFileVersion("/Users/user/.apl-blockchain/apl-blockchain-vault-keystore/a2e9b9/vd_2019-03-19_13-58-16---APL-9ES6-JTW3-JBN4-7LLZQ");
        Integer version2 = FbWalletUtil.getWalletFileVersion("/Users/user/.apl-blockchain/apl-blockchain-vault-keystore/a2e9b9/1_2019-03-19_13-58-16---APL-9ES6-JTW3-JBN4-7LLZQ");

        assertNull(version1);
        assertNull(version2);
    }

    @Test
    void getAccount() {
        String account1 = FbWalletUtil.getAccount("/Users/user/.apl-blockchain/apl-blockchain-vault-keystore/a2e9b9/v1_2019-03-19_13-58-16---APL-9ES6-JTW3-JBN4-7LLZQ");
        String account0 = FbWalletUtil.getAccount("/Users/user/.apl-blockchain/apl-blockchain-vault-keystore/a2e9b9/v0_2019-03-19_13-58-16---APL-9ES6-JTW3-JBN4-7LLZ1");

        assertTrue(Objects.equals("APL-9ES6-JTW3-JBN4-7LLZQ", account1));
        assertTrue(Objects.equals("APL-9ES6-JTW3-JBN4-7LLZ1", account0));
    }

    @Test
    void getAccountWhenFileNameNotValid() {
        String account = FbWalletUtil.getAccount("v1_2019-03-19_13-58-16-APL-9ES6-JTW3-JBN4-7LLZ");

        assertNull(account);
    }
}