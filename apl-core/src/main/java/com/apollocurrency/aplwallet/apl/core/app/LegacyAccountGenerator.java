/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.account.GeneratedAccount;
import com.apollocurrency.aplwallet.apl.core.account.AccountGenerator;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;

public class LegacyAccountGenerator implements AccountGenerator {
    private PassphraseGenerator passphraseGenerator;

    public LegacyAccountGenerator(PassphraseGenerator passphraseGenerator) {
        this.passphraseGenerator = passphraseGenerator;
    }

    public LegacyAccountGenerator() {
    }

    @Override
    public GeneratedAccount generate(String passphrase) {
        if (passphrase == null) {
            if (passphraseGenerator == null) {
                throw new RuntimeException("Either passphrase generator or passphrase required");
            }
            passphrase = passphraseGenerator.generate();
        }
        byte[] secretBytes = new byte[32];
        Crypto.getSecureRandom().nextBytes(secretBytes);
        byte[] keySeed = Crypto.getKeySeed(secretBytes);
        byte[] privateKey = Crypto.getPrivateKey(keySeed);
        byte[] accountPublicKey = Crypto.getPublicKey((keySeed));
        long accountId = Convert.getId(accountPublicKey);
        return new GeneratedAccount(accountId, accountPublicKey, privateKey, passphrase, secretBytes);
    }
//-7883855003351141204
    public PassphraseGenerator getPassphraseGenerator() {
        return passphraseGenerator;
    }

    public void setPassphraseGenerator(PassphraseGenerator passphraseGenerator) {
        this.passphraseGenerator = passphraseGenerator;
    }
}
