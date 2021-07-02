/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.exception;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;

/**
 * Exception, that may be thrown during transaction execution indicating that it's acceptable and expected  behavior,
 * so that transaction should be cared as failed
 * @author Andrii Boiarskyi
 * @see AplTransactionException
 * @since 1.48.4
 */
public class AplTransactionExecutionException extends AplTransactionException {
    public AplTransactionExecutionException(String message, Transaction tx) {
        super(message, tx);
    }

    public AplTransactionExecutionException(String message, Throwable cause, Transaction tx) {
        super(message, cause, tx);
    }
}
