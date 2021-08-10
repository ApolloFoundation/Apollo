/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.exception.AplCoreLogicException;

/**
 * General MemPool exception used to throw or wrap in exceptions inside the {@link MemPool}
 * @author Andrii Boiarskyi
 * @see MemPool
 * @see AplCoreLogicException
 * @since 1.48.4
 */
public class AplMemPoolException extends AplCoreLogicException {
    public AplMemPoolException(String message) {
        super(message);
    }

    public AplMemPoolException(String message, Throwable cause) {
        super(message, cause);
    }
}
