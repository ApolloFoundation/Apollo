/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.utils;

import com.apollocurrency.aplwallet.apl.core.model.AplWalletKey;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.eth.model.EthWalletKey;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class AccountGeneratorUtil {
    private static final Logger log = getLogger(AccountGeneratorUtil.class);

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

    /**
     * Generate new account with random key.
     *
     * @return EthWallet
     */
    public static EthWalletKey generateEth() {
        return EthUtil.generateNewAccount();
    }
}
