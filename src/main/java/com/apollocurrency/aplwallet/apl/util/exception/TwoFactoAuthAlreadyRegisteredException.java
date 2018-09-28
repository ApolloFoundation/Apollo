/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.exception;

public class TwoFactoAuthAlreadyRegisteredException extends AbstractTwoFactorAuthException {
    public TwoFactoAuthAlreadyRegisteredException(String message) {
        super(message);
    }

    public TwoFactoAuthAlreadyRegisteredException(String message, Throwable cause) {
        super(message, cause);
    }
}
