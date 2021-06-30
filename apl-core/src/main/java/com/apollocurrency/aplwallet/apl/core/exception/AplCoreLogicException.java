/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.exception;

/**
 * <p>
 *      Core logic base exception, should be used for the exception throwing/handling
 *      in a new/refactored core logic code instead of
 *      {@link com.apollocurrency.aplwallet.apl.util.exception.AplException} when possible
 * </p>
 * <p>
 *     For each type of exception case a separate exception class should be created
 * </p>
 *
 * @author Andrii Boiarskyi
 * @see com.apollocurrency.aplwallet.apl.util.exception.AplException
 * @since 1.48.4
 */
public class AplCoreLogicException extends RuntimeException {

    public AplCoreLogicException(String message) {
        super(message);
    }

    public AplCoreLogicException(String message, Throwable cause) {
        super(message, cause);
    }
}
