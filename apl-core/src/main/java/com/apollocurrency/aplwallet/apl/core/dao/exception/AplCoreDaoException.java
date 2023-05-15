/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.exception;

/**
 * <p>
 *  Base Apollo Core DAO exception, which should be used inside all DAO classes to throw/wrap exceptions.
 * </p>
 * <p>
 *  Designed especially to replace direct RuntimeException/SQLException throwing and form consistent and integral
 *  layer of DAO exception management
 * </p>
 *
 * @author Andrii Boiarskyi
 * @see com.apollocurrency.aplwallet.apl.core.exception.AplCoreLogicException
 * @see java.sql.SQLException
 * @see RuntimeException
 * @since 1.48.4
 */
public class AplCoreDaoException extends RuntimeException {
    public AplCoreDaoException(String message) {
        super(message);
    }

    public AplCoreDaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
