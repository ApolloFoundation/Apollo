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
public class SignatureCredential implements Credential {
    private final byte[] publicKey;

    public SignatureCredential(byte[] publicKey) {
        this.publicKey = Objects.requireNonNull(publicKey);
    }

}
