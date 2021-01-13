/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.exception;

/**
 * @author al
 */
public class DoubleSpendingException extends RuntimeException {

    public DoubleSpendingException(String message, long accountId, long confirmed, long unconfirmed) {
        super(message + " account: " + Long.toUnsignedString(accountId) + " confirmed: " + confirmed + " unconfirmed: " + unconfirmed);
    }

}
