/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

/**
 * Thrown, when mempool max size is reached and no transactions can be added
 * @author Andrii Boiarskyi
 * @see MemPool
 * @see AplMemPoolException
 * @since 1.48.4
 */
public class AplMemPoolFullException extends AplMemPoolException {
    public AplMemPoolFullException(String message) {
        super(message);
    }

    public AplMemPoolFullException(String message, Throwable cause) {
        super(message, cause);
    }
}
