/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
public class UnsupportedTransactionVersion extends RuntimeException {
    public UnsupportedTransactionVersion() {
    }

    public UnsupportedTransactionVersion(String message) {
        super(message);
    }

    public UnsupportedTransactionVersion(String message, Throwable cause) {
        super(message, cause);
    }
}
