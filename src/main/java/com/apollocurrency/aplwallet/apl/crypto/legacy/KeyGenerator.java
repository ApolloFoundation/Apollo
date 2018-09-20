package com.apollocurrency.aplwallet.apl.crypto.legacy;

import com.apollocurrency.aplwallet.apl.crypto.asymmetric.AsymmetricKeyGenerator;

import java.security.KeyPair;

public class KeyGenerator implements AsymmetricKeyGenerator {

    @Override
    public KeyPair generateKeyPair(String secretPhrase) {
        java.security.PublicKey publicKey = new PublicKey(Crypto.getPublicKey(secretPhrase));
        java.security.PrivateKey privateKey = new PrivateKey(Crypto.getPrivateKey(secretPhrase));
        return new KeyPair(publicKey, privateKey);
    }

}
