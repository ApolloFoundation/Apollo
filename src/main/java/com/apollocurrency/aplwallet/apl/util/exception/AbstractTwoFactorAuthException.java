/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.exception;

public abstract class AbstractTwoFactorAuthException extends RuntimeException {
    public AbstractTwoFactorAuthException(String message) {
        super(message);
    }

    public AbstractTwoFactorAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
