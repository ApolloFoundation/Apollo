/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.model.AplWalletKey;
import com.apollocurrency.aplwallet.apl.core.account.AccountGenerator;
import com.apollocurrency.aplwallet.apl.core.model.EthWalletKey;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import org.slf4j.Logger;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;

import static org.slf4j.LoggerFactory.getLogger;

public class AccountGeneratorImpl implements AccountGenerator {
    private static final Logger log = getLogger(AccountGeneratorImpl.class);

    private PassphraseGenerator passphraseGenerator;

    public AccountGeneratorImpl(PassphraseGenerator passphraseGenerator) {
        this.passphraseGenerator = passphraseGenerator;
    }

    public AccountGeneratorImpl() {
    }

    @Override
    public AplWalletKey generateApl() {
        byte[] secretBytes = new byte[32];
        Crypto.getSecureRandom().nextBytes(secretBytes);
        byte[] keySeed = Crypto.getKeySeed(secretBytes);
        byte[] privateKey = Crypto.getPrivateKey(keySeed);
        byte[] accountPublicKey = Crypto.getPublicKey((keySeed));
        long accountId = Convert.getId(accountPublicKey);
        return new AplWalletKey(accountId, accountPublicKey, privateKey, secretBytes);
    }

    @Override
    public EthWalletKey generateEth() {
        byte[] secretBytes = new byte[32];
        Crypto.getSecureRandom().nextBytes(secretBytes);
        byte[] keySeed = Crypto.getKeySeed(secretBytes);
        byte[] privateKey = Crypto.getPrivateKey(keySeed);

        ECKeyPair ecKeyPair = ECKeyPair.create(privateKey);
        Credentials cs = Credentials.create(ecKeyPair);

//        String privateKey = cs.getEcKeyPair().getPrivateKey().toString(16);
//        String publicKey = cs.getEcKeyPair().getPublicKey().toString(16);
//        String addr = cs.getAddress();
        return new EthWalletKey(cs, keySeed);
    }

    public PassphraseGenerator getPassphraseGenerator() {
        return passphraseGenerator;
    }

    public void setPassphraseGenerator(PassphraseGenerator passphraseGenerator) {
        this.passphraseGenerator = passphraseGenerator;
    }
}
