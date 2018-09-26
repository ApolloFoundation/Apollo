/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.exception;

public class TwoFactoAuthAlreadyEnabledException extends AbstractTwoFactorAuthException {
    public TwoFactoAuthAlreadyEnabledException(String message) {
        super(message);
    }

    public TwoFactoAuthAlreadyEnabledException(String message, Throwable cause) {
        super(message, cause);
    }
}
