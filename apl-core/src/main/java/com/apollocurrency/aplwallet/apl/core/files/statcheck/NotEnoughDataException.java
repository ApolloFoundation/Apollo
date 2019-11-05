/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.files.statcheck;

/**
 *
 * @author alukin@gmail.com
 */
public class NotEnoughDataException extends Exception {

    /**
     * Creates a new instance of <code>NotEnoughDataException</code> without
     * detail message.
     */
    public NotEnoughDataException() {
    }

    /**
     * Constructs an instance of <code>NotEnoughDataException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public NotEnoughDataException(String msg) {
        super(msg);
    }
}
