/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.exception;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import lombok.NonNull;

/**
 * Exception, which refers a situation, when transaction failed during execution but its transaction
 * type does not support such failures
 * @author Andrii Boiarskyi
 * @see AplTransactionException
 * @see AplTransactionExecutionException
 * @since 1.48.4
 */
public class AplTransactionExecutionFailureNotSupportedException extends AplTransactionException {
    public AplTransactionExecutionFailureNotSupportedException(@NonNull Transaction tx) {
        super(createErrorMessage(tx), tx);
    }

    public AplTransactionExecutionFailureNotSupportedException(@NonNull Throwable cause, @NonNull Transaction tx) {
        super(createErrorMessage(tx), cause, tx);
    }

    private static String createErrorMessage(Transaction tx) {
        return "Transaction " + tx.getStringId() + " failure during execution is not supported for tx type: " + tx.getType().getSpec() + " at height " + tx.getHeight();
    }
}
