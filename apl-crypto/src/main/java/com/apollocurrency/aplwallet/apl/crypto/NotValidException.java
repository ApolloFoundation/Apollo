package com.apollocurrency.aplwallet.apl.crypto;

/**
 * @author al
 */
public class NotValidException extends Exception {

    /**
     * Creates a new instance of <code>NotValidException</code> without detail
     * message.
     */
    public NotValidException() {
    }

    /**
     * Constructs an instance of <code>NotValidException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public NotValidException(String msg) {
        super(msg);
    }
}
