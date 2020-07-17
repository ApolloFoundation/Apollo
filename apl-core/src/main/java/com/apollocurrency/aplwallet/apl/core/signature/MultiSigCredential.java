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
    private final byte[][] keys;

    public MultiSigCredential(int threshold, byte[][] keys) {
        this.keys = Objects.requireNonNull(keys);
        if (threshold < 1 || threshold > keys.length) {
            throw new IllegalArgumentException("Wrong threshold value.");
        }
        this.threshold = threshold;
    }

    public MultiSigCredential(byte[][] keys) {
        this(keys.length, keys);
    }

    public MultiSigCredential(byte[] key) {
        this(1, new byte[][]{key});
    }

    @Override
    public boolean validateCredential(KeyValidator validator) {
        for (byte[] pk : keys) {
            if (!validator.validate(pk)) {
                return false;
            }
        }
        return true;
    }
}
