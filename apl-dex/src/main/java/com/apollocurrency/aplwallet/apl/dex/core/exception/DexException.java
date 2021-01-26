package com.apollocurrency.aplwallet.apl.dex.core.exception;

public class DexException extends RuntimeException {

    public DexException() {
        super();
    }

    public DexException(String message) {
        super(message);
    }

    public DexException(String message, Throwable cause) {
        super(message, cause);
    }

    public DexException(Throwable cause) {
        super(cause);
    }
}
