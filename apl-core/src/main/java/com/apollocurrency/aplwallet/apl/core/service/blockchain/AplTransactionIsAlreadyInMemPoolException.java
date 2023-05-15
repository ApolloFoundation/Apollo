/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

/**
 * Thrown, when transaction is already exist inside the mempool
 * @author Andrii Boiarskyi
 * @see MemPool
 * @see AplMemPoolException
 * @since 1.48.4
 */
public class AplTransactionIsAlreadyInMemPoolException extends AplMemPoolException {
    public AplTransactionIsAlreadyInMemPoolException(String message) {
        super(message);
    }

    public AplTransactionIsAlreadyInMemPoolException(String message, Throwable cause) {
        super(message, cause);
    }
}
