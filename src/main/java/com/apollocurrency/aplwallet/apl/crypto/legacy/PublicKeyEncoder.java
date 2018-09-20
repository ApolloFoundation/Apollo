package com.apollocurrency.aplwallet.apl.crypto.legacy;

import java.security.InvalidParameterException;
import java.security.PublicKey;

public class PublicKeyEncoder implements com.apollocurrency.aplwallet.apl.crypto.asymmetric.PublicKeyEncoder {

    @Override
    public byte[] encode(PublicKey key) {
        if(!com.apollocurrency.aplwallet.apl.crypto.legacy.PublicKey.FORMAT.equals(key.getFormat())) {
            throw new InvalidParameterException("Invalid public key format. Check crypto config");
        }
        return key.getEncoded();
    }

    @Override
    public PublicKey decode(byte[] bytes) {
        if(bytes.length != getEncodedLength()) {
            throw new InvalidParameterException("Invalid public key format. Check crypto config");
        }
        return new com.apollocurrency.aplwallet.apl.crypto.legacy.PublicKey(bytes);
    }

    @Override
    public int getEncodedLength() {
        return 32;
    }

}
