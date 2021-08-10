/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.exception;

/**
 * Indicates that method/class contract was violated and class creation or method invocation with some results should not be possible
 * <p>
 *     This exception is a good replacement for {@link IllegalStateException} or {@link IllegalArgumentException} within apl-core module
 * </p>
 * @author Andrii Boiarskyi
 * @see AplCoreLogicException
 * @since 1.48.4
 */
public class AplCoreContractViolationException extends AplCoreLogicException {
    public AplCoreContractViolationException(String message) {
        super(message);
    }

    public AplCoreContractViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
