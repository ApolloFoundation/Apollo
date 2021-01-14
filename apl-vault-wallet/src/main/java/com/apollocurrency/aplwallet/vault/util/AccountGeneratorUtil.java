/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.vault.util;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.vault.model.AplWalletKey;

public class AccountGeneratorUtil {

    public AccountGeneratorUtil() {
    }

    /**
     * Generate new account with random key.
     *
     * @return AplWallet
     */
    public static AplWalletKey generateApl() {
        byte[] secretBytes = new byte[32];
        Crypto.getSecureRandom().nextBytes(secretBytes);
        byte[] keySeed = Crypto.getKeySeed(secretBytes);
        byte[] privateKey = Crypto.getPrivateKey(keySeed);
        byte[] accountPublicKey = Crypto.getPublicKey((keySeed));
        long accountId = Convert.getId(accountPublicKey);
        return new AplWalletKey(accountId, accountPublicKey, privateKey, secretBytes);
    }

}
