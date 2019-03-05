/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account;

/**
 *
 * @author al
 */
class DoubleSpendingException extends RuntimeException {
    
    DoubleSpendingException(String message, long accountId, long confirmed, long unconfirmed) {
        super(message + " account: " + Long.toUnsignedString(accountId) + " confirmed: " + confirmed + " unconfirmed: " + unconfirmed);
    }
    
}
