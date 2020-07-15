/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

import lombok.Getter;

import java.util.Objects;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@Getter
public class MultiSigCredential implements Credential {
    private final int threshold;
    private final byte[][] publicKeys;

    public MultiSigCredential(int threshold, byte[][] publicKeys) {
        this.threshold = threshold;
        this.publicKeys = Objects.requireNonNull(publicKeys);
        if (threshold < 1 || threshold > publicKeys.length) {
            throw new IllegalArgumentException("Wrong threshold value.");
        }
    }

    public MultiSigCredential(byte[] pubicKey) {
        this(1, new byte[][]{pubicKey});
    }

    @Override
    public boolean validateCredential(PublicKeyValidator validator) {
        for (byte[] pk : publicKeys) {
            if (!validator.validate(pk)) {
                return false;
            }
        }
        return true;
    }
}
