/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

/**
 * @author andrii.zincheko@firstbridge.io
 */
public class SignatureParseException extends RuntimeException {
    public SignatureParseException() {
    }

    public SignatureParseException(String message) {
        super(message);
    }

    public SignatureParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public SignatureParseException(Throwable cause) {
        super(cause);
    }
}
