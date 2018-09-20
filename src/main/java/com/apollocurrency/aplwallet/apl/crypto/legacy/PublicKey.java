package com.apollocurrency.aplwallet.apl.crypto.legacy;

import java.util.Arrays;

public class PublicKey implements java.security.PublicKey {

    static final String ALGORITHM = "apl-crypto-legacy-public-key";
    static final String FORMAT = "key-public-binary";

    private final byte[] key;

    public PublicKey(byte[] key) {
        this.key = key;
    }

    @Override
    public String getAlgorithm() {
        return ALGORITHM;
    }

    @Override
    public String getFormat() {
        return FORMAT;
    }

    @Override
    public byte[] getEncoded() {
        return Arrays.copyOf(key, key.length);
    }
}
