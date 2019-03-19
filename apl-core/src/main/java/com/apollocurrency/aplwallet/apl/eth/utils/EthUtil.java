package com.apollocurrency.aplwallet.apl.eth.utils;

import com.apollocurrency.aplwallet.apl.eth.model.EthWalletKey;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;

public class EthUtil {

    /**
     * Generate new account with random key.
     * @return EthWallet
     */
    public static EthWalletKey generateNewAccount(){
        byte[] secretBytes = new byte[32];
        Crypto.getSecureRandom().nextBytes(secretBytes);
        byte[] keySeed = Crypto.getKeySeed(secretBytes);
        byte[] privateKey = Crypto.getPrivateKey(keySeed);

        ECKeyPair ecKeyPair = ECKeyPair.create(privateKey);
        Credentials cs = Credentials.create(ecKeyPair);

//        String privateKey = cs.getEcKeyPair().getPrivateKey().toString(16);
//        String publicKey = cs.getEcKeyPair().getPublicKey().toString(16);
//        String addr = cs.getAddress();
        return new EthWalletKey(cs);
    }

}
