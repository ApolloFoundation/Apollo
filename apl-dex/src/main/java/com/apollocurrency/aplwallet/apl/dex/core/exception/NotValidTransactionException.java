package com.apollocurrency.aplwallet.apl.dex.core.exception;

public class NotValidTransactionException extends RuntimeException {

    public NotValidTransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotValidTransactionException(String message) {
        super(message);
    }
}
