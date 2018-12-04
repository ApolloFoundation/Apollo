/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.exception;

public class InvalidTwoFactorAuthCredentialsException extends AbstractTwoFactorAuthException {
    public InvalidTwoFactorAuthCredentialsException(String message) {
        super(message);
    }

    public InvalidTwoFactorAuthCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
