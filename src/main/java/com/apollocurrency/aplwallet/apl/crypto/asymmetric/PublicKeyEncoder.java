package com.apollocurrency.aplwallet.apl.crypto.asymmetric;

/**
 * Interface for PublicKeyEncoder component
 */
public interface PublicKeyEncoder {

    /**
     * get byte[] representation of Public Key without any metadata
     * @param key
     * @return
     */
    byte[] encode(java.security.PublicKey key);

    /**
     * Decode previously encoded using same KeyEncoder key
     * @param bytes
     * @return
     */
    java.security.PublicKey decode(byte[] bytes);

    /**
     * Length in bytes of the encoded key data
     * this should basically return a constant value
     * @return
     */
    int getEncodedLength();

}
