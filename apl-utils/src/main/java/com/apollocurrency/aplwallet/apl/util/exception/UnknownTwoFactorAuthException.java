/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.exception;

public class UnknownTwoFactorAuthException extends AbstractTwoFactorAuthException {
    public UnknownTwoFactorAuthException(String message) {
        super(message);
    }

    public UnknownTwoFactorAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
