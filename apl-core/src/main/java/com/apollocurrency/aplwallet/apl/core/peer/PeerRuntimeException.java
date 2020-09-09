/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.peer;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
public class PeerRuntimeException extends RuntimeException {
    public PeerRuntimeException() {
    }

    public PeerRuntimeException(String message) {
        super(message);
    }

    public PeerRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
