/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.exception.AplCoreLogicException;

/**
 * Thrown, when unable to broadcast transaction for some reason
 * @author Andrii Boiarskyi
 * @see TransactionProcessorImpl
 * @since 1.48.4
 */
public class AplTransactionBroadcastException extends AplCoreLogicException {
    public AplTransactionBroadcastException(String message) {
        super(message);
    }

    public AplTransactionBroadcastException(String message, Throwable cause) {
        super(message, cause);
    }
}
