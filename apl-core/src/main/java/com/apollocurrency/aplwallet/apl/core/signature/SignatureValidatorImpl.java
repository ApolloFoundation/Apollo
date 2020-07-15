/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@Slf4j
public class SignatureValidatorImpl implements SignatureValidator {

    private final byte[] document;

    public SignatureValidatorImpl(byte[] document) {
        this.document = Objects.requireNonNull(document);
    }

    @Override
    public boolean verify(Signature signature, Credential credential) {
        SignatureCredential signatureCredential;
        if (credential instanceof SignatureCredential) {
            signatureCredential = (SignatureCredential) credential;
        } else {
            throw new IllegalArgumentException("Can't cast credential object to SignatureCredential type.");
        }
        return Crypto.verify(signature.bytes(), document, signatureCredential.getPublicKey());
    }

}
