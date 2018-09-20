package com.apollocurrency.aplwallet.apl.crypto.legacy;

import java.security.PrivateKey;
import java.security.PublicKey;

public class Signer implements com.apollocurrency.aplwallet.apl.crypto.asymmetric.signature.Signer {

    @Override
    public byte[] sign(byte[] message, PrivateKey privateKey) {
        return Crypto.signWithPrivateKey(message, privateKey.getEncoded());
    }

    @Override
    public boolean verify(byte[] message, byte[] signature, PublicKey theirPublicKey) {
        return Crypto.verify(signature, message, theirPublicKey.getEncoded());
    }

    @Override
    public int getSignatureLength() {
        return 64;
    }
}
