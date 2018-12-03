/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Convert;

public class TestAccount extends BasicAccount {
    private String secretPhrase;

    public TestAccount(long account, String secretPhrase) {
        super(account);
        this.secretPhrase = secretPhrase;
    }
    public TestAccount(String secretPhrase) {
        super(Convert.getId(Crypto.getPublicKey(secretPhrase)));
        this.secretPhrase = secretPhrase;
    }

    public String getSecretPhrase() {
        return secretPhrase;
    }
}
