/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.exception;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import lombok.NonNull;

/**
 * <p>
 *     General transaction validation exception, indicating that transaction validation failed for some reason.
 *     Derived classes should clarify validation cases: invalid transaction by data, by system, by state
 * </p>
 *
 * <p>
 *     Should be used for a
 *     {@link com.apollocurrency.aplwallet.apl.util.exception.AplException.ValidationException} replacement in a new and
 *     refactored code.
 * </p>

 * @author Andrii Boiarskyi
 * @see AplTransactionException
 * @see com.apollocurrency.aplwallet.apl.util.exception.AplException.ValidationException
 * @since 1.48.4
 */
public abstract class AplTransactionValidationException extends AplTransactionException {
    public AplTransactionValidationException(String message, @NonNull Transaction tx) {
        super(message, tx);
    }

    public AplTransactionValidationException(String message, @NonNull Throwable cause, @NonNull Transaction tx) {
        super(message, cause, tx);
    }
}
