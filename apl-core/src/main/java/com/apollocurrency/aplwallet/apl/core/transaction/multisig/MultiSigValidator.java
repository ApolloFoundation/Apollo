/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.multisig;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
public interface MultiSigValidator {

    default boolean verify(byte[]... pubicKeys) {
        return verify(pubicKeys.length, pubicKeys);
    }

    boolean verify(int threshold, byte[]... pubicKeys);

}
