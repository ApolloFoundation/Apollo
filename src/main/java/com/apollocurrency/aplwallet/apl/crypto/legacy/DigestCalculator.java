package com.apollocurrency.aplwallet.apl.crypto.legacy;

import java.security.MessageDigest;

public class DigestCalculator implements com.apollocurrency.aplwallet.apl.crypto.DigestCalculator {

    @Override
    public byte[] calcDigest(byte[] message) {
        return Crypto.sha256().digest(message);
    }

    @Override
    public MessageDigest createDigest() {
        return Crypto.sha256();
    }

    @Override
    public int getCalculatedLength() {
        return 32;
    }

}
