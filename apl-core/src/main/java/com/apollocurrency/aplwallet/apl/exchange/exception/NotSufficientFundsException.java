package com.apollocurrency.aplwallet.apl.exchange.exception;

public class NotSufficientFundsException extends RuntimeException {
    public NotSufficientFundsException(String message) {
        super(message);
    }

    public NotSufficientFundsException(String message, Throwable cause) {
        super(message, cause);
    }
}
