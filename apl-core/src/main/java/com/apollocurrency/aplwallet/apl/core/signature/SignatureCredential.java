/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@Getter
public class SignatureCredential implements Credential {
    private final byte[] key;

    public SignatureCredential(byte[] key) {
        this.key = Objects.requireNonNull(key);
    }

    @Override
    public boolean validateCredential(KeyValidator validator) {
        return validator.validate(key);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SignatureCredential.class.getSimpleName() + "[", "]")
            .add("key=" + Convert.toHexString(key))
            .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SignatureCredential that = (SignatureCredential) o;
        return Arrays.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(key);
    }
}
