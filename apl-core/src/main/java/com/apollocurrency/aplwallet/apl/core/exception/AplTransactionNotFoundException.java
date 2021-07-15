/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.exception;

import com.apollocurrency.aplwallet.apl.core.service.blockchain.FailedTransactionVerificationService;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import lombok.Getter;

/**
 * Exception, which indicates that transaction with specified id was not found
 * @author Andrii Boiarskyi
 * @see FailedTransactionVerificationService
 * @since 1.48.4
 */
public class AplTransactionNotFoundException extends AplCoreLogicException {
    @Getter
    private final long id;
    public AplTransactionNotFoundException(long id, String additionalInfo) {
        super(notFoundTransaction(id, additionalInfo));
        this.id = id;
    }

    public AplTransactionNotFoundException(long id, String additionalInfo, Throwable cause) {
        super(notFoundTransaction(id, additionalInfo), cause);
        this.id = id;
    }

    private static String notFoundTransaction(long id, String additionalInfo) {
        return "Transaction with id '" + Long.toUnsignedString(id) + "' was not found" + (StringUtils.isNotBlank(additionalInfo) ? ", details: '" + additionalInfo + "'": "");
    }
}
