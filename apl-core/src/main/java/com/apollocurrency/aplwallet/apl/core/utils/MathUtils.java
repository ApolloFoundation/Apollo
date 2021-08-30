/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.utils;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;

/**
 * Contains math operations with overflow validation
 * @author Andrii Boiarskyi
 * @see com.apollocurrency.aplwallet.apl.util.Convert2
 * @see com.apollocurrency.aplwallet.apl.util.exception.AplException.NotValidException
 * @see com.apollocurrency.aplwallet.apl.core.transaction.types.ms.MSPublishExchangeOfferTransactionType
 * @since 1.48.4
 */
public class MathUtils {
    /**
     * Ensure, that multiplication of the two given values, x and y, will not exceed {@link Long} java type limits and return result
     * of that multiplication, otherwise throw exception with details appended by the given message
     *
     * @param x       first value to multiply
     * @param y       second value to multiply by first value
     * @param message error message, which will be attached to an exception in a case of failed multiplication
     * @return multiplication result
     * @throws AplException.NotValidException when multiplication result exceeds {@link Long} java type limits
     */
    public static long safeMultiply(long x, long y, String message) throws AplException.NotValidException {
        try {
            return Math.multiplyExact(x, y);
        } catch (ArithmeticException e) {
            throw new AplException.NotValidException("Result of multiplying x=" + x + ", y=" + y + " exceeds the allowed range [" + Long.MIN_VALUE + ";" + Long.MAX_VALUE + "], " + message, e);
        }
    }

    /**
     * Do the same as {@link MathUtils#safeMultiply(long, long, String)} but construct error message from the given transaction object
     *
     * @param x  same as for {@link MathUtils#safeMultiply(long, long, String)}
     * @param y  same as for {@link MathUtils#safeMultiply(long, long, String)}
     * @param tx transaction, for which error message will be constructed using transaction id, sender's id and transaction type spec
     * @return same as {@link MathUtils#safeMultiply(long, long, String)}
     * @throws AplException.NotValidException when {@link MathUtils#safeMultiply(long, long, String)} throws
     */
    public static long safeMultiply(long x, long y, Transaction tx) throws AplException.NotValidException {
        return safeMultiply(x, y, "transaction='" + tx.getStringId() + "', type='" + tx.getType().getSpec() + "', sender='" + Long.toUnsignedString(tx.getSenderId()) + "'");
    }
}
