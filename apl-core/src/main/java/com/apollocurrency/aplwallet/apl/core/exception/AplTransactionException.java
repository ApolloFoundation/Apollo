/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.exception;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.util.StringValidator;
import lombok.Getter;
import lombok.NonNull;

/**
 * <p>
 * Base exception class for the transaction processing exceptions.
 * </p>
 * <p>
 * Does not accept null/empty messages to allow exception details saving during transaction acceptance
 * </p>
 * <p>
 * An exception's creator should supply a meaningful, unique and self-descriptive message to trace an error
 * even without stacktrace
 * </p>
 *
 * @author Andrii Boiarskyi
 * @see AplCoreLogicException
 * @see AplTransactionExecutionException
 * @see AplAcceptableTransactionValidationException
 * @since 1.48.4
 */
public abstract class AplTransactionException extends AplCoreLogicException {
    @Getter
    private final Transaction tx;
    public AplTransactionException(String message, @NonNull Transaction tx) {
        super(message);
        this.tx = tx;
        StringValidator.requireNonBlank(message, "message");
    }

    public AplTransactionException(String message, @NonNull Throwable cause, @NonNull Transaction tx) {
        super(message, cause);
        this.tx = tx;
        StringValidator.requireNonBlank(message, "message");
    }

    @Override
    public String toString() {
        return "Transaction '" + tx.toString() + "' failed with message: '" + getMessage() + "'";
    }
}
