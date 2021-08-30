/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.exception;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import lombok.NonNull;

/**
 * Exception, for cases when some feature is not yet enabled
 * <p>
 *     Designated mostly to replace legacy AplException.NotYetEnabledException
 * </p>
 * <p>
 *     For example: creation of a transaction of a new type or in a new form/version;
 *
 * </p>
 * @author Andrii Boiarskyi
 * @see AplCoreLogicException
 * @since 1.48.4
 */
public class AplTransactionFeatureNotEnabledException extends AplUnacceptableTransactionValidationException {

    public AplTransactionFeatureNotEnabledException(@NonNull String feature, @NonNull Transaction tx) {
        super(formExMessage(feature, tx), tx);
    }

    public AplTransactionFeatureNotEnabledException(@NonNull String feature, @NonNull Throwable cause, @NonNull Transaction tx) {
        super(formExMessage(feature, tx), cause, tx);
    }

    private static String formExMessage(String feature, Transaction transaction) {
        return "Feature '" + feature + "' is not enabled yet for transaction " + transaction.getStringId() + " of type " + transaction.getType().getSpec();
    }
}
