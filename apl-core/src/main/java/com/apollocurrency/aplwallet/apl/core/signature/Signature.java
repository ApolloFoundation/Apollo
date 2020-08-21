/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

/**
 * It's a simple interface for signature
 */
public interface Signature {
    int ECDSA_SIGNATURE_SIZE = 64;

    byte[] bytes();

    int getSize();

    /**
     * Returns signature byte array as a hex string
     *
     * @return hex string representation of signature
     */
    String getHexString();

    /**
     * Return true if signature is already verified.
     * Used to increase the speed of the verification routine and considered as a simple cache
     *
     * @return
     */
    boolean isVerified();

}
