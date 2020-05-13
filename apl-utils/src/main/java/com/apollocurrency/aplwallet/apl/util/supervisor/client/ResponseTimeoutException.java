package com.apollocurrency.aplwallet.apl.util.supervisor.client;

/**
 * Represent exception occurred during sending request, when sender did not receive response for some amount of time
 */
public class ResponseTimeoutException extends RuntimeException{
    public ResponseTimeoutException(String message) {
        super(message);
    }

    public ResponseTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
