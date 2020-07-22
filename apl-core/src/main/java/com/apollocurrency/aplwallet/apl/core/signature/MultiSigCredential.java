/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

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

    @Override
    public String toString() {
        return new StringJoiner(", ", MultiSigCredential.class.getSimpleName() + "[", "]")
            .add("threshold=" + threshold)
            .add("keys=[" + Arrays.stream(keys).map(Convert::toHexString).collect(Collectors.joining(",")) + "]")
            .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MultiSigCredential that = (MultiSigCredential) o;
        return threshold == that.threshold &&
            Arrays.equals(keys, that.keys);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(threshold);
        result = 31 * result + Arrays.hashCode(keys);
        return result;
    }
}
