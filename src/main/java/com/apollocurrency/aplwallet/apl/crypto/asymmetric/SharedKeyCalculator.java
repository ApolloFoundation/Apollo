package com.apollocurrency.aplwallet.apl.crypto.asymmetric;

public interface SharedKeyCalculator {

    /**
     * Calculate shared key
     * @param myPublicKey
     * @param myPrivateKey
     * @param theirPublicKey
     * @return
     */
    byte[] calcSharedKey(java.security.PublicKey myPublicKey, java.security.PrivateKey myPrivateKey, java.security.PublicKey theirPublicKey);

    /**
     * length in bytes if the calculated key
     * @return
     */
    int getCalculatedLength();

}