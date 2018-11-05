/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.exception;

/**
 * Unchecked exception class, used for wrapping database exceptions or indicate general db errors
 */
public class DbException extends RuntimeException {
    public DbException(String message) {
        super(message);
    }

    public DbException(String message, Throwable cause) {
        super(message, cause);
    }
}
