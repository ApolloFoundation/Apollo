/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.exception;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;

/**
 * <p>
 * Exception, that may be thrown during transaction validation and means
 * that transaction is invalid and can not be accepted by a network
 * </p>
 * @author Andrii Boiarskyi
 * @see AplTransactionValidationException
 * @see AplAcceptableTransactionValidationException
 * @since 1.48.4
 */
public class AplUnacceptableTransactionValidationException extends AplTransactionValidationException {
    public AplUnacceptableTransactionValidationException(String message, Transaction tx) {
        super(message, tx);
    }

    public AplUnacceptableTransactionValidationException(String message, Throwable cause, Transaction tx) {
        super(message, cause, tx);
    }
}
