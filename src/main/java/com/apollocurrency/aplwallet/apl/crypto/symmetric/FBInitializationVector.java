package com.apollocurrency.aplwallet.apl.crypto.symmetric;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Initialization vector variable part, 4+8=12 bytes, or salt and
 * explicit_nounce used to init GCM. So it could be 4 bytes of "fixed"
 * nounce or full 12 bytes. In case of 4 bytes random 8 bytes generated for
 * nonce_explicit From RFC 5288: AES-GCM security requires that the counter
 * is never reused. The IV construction in Section 3 is designed to prevent
 * counter reuse. Implementers should also understand the practical
 * considerations of IV handling outlined in Section 9 of [GCM]. In this
 * class IV is 12 bytes as defined in RFC 5116 struct { opaque salt[4];
 * opaque nonce_explicit[8]; } GCMNonce; Salt is "fixed" part of IV and
 * comes with key, nounce_explicit is "variable" part of IV and comes with
 * message. So IV in this method should be 12 bytes long
 */
public class FBInitializationVector implements InitializationVector {

    private final byte[] vector;

    public FBInitializationVector(byte[] vector) {
        if(vector.length != 12) {
            throw new IllegalArgumentException("Initialization Vector must be exactly 12 bytes long");
        }
        this.vector = vector;
    }

    public FBInitializationVector(byte[] salt, byte[] nonce) {
        if(salt.length != 4) {
            throw new IllegalArgumentException("Salt must be 4 bytes long");
        }
        if(nonce.length != 8) {
            throw new IllegalArgumentException("Nonce must be 8 bytes long");
        }
        this.vector = new byte[12];
        ByteBuffer.wrap(this.vector).put(salt).put(nonce);
    }

    @Override
    public byte[] getSalt() {
        return Arrays.copyOf(vector, 4);
    }

    @Override
    public byte[] getNonce() {
        return Arrays.copyOfRange(vector, 4, 12);
    }

    @Override
    public byte[] getBytes() {
        return vector;
    }

}
