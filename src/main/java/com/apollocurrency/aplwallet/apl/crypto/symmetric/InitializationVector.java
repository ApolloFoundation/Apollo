package com.apollocurrency.aplwallet.apl.crypto.symmetric;

public interface InitializationVector {

    /**
     * Subset of IV designating Salt
     * @return
     */
    byte[] getSalt();

    /**
     * Subset of IV designating Nonce
     * @return
     */
    byte[] getNonce();

    /**
     * IV itself
     * @return
     */
    byte[] getBytes();

}