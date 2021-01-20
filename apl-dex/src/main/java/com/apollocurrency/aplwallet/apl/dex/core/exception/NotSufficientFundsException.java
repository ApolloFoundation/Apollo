package com.apollocurrency.aplwallet.apl.dex.core.exception;

public class NotSufficientFundsException extends RuntimeException {
    public NotSufficientFundsException(String message) {
        super(message);
    }

    public NotSufficientFundsException(String message, Throwable cause) {
        super(message, cause);
    }
}
