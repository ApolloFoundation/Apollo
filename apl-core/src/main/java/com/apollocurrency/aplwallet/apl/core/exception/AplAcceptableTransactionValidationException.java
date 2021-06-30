/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.exception;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;

/**
 * <p>
 * Exception, that may be thrown during transaction validation and means
 * that transaction may be accepted by the node in a failed state without execution
 * </p>
 * @author Andrii Boiarskyi
 * @see AplTransactionValidationException
 * @see AplUnacceptableTransactionValidationException
 * @since 1.48.4
 */
public class AplAcceptableTransactionValidationException extends AplTransactionValidationException {
    public AplAcceptableTransactionValidationException(String message, Transaction tx) {
        super(message, tx);
    }

    public AplAcceptableTransactionValidationException(String message, Throwable cause, Transaction tx) {
        super(message, cause, tx);
    }
}
