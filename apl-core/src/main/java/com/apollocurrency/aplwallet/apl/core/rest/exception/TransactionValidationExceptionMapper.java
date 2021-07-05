/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.exception;

import com.apollocurrency.aplwallet.apl.core.exception.AplTransactionException;
import com.apollocurrency.aplwallet.apl.util.builder.ResponseBuilder;
import com.apollocurrency.aplwallet.apl.util.exception.ApiErrors;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * Designed to catch all the transaction validation exceptions derived from the {@link com.apollocurrency.aplwallet.apl.core.exception.AplTransactionValidationException}
 * and produced mostly by the {@link com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator} methods
 * and in exceptional cases: transaction execution errors derived from the {@link com.apollocurrency.aplwallet.apl.core.exception.AplTransactionExecutionException}
 * @author Andrii Boiarskyi
 * @see com.apollocurrency.aplwallet.apl.core.exception.AplTransactionValidationException
 * @see com.apollocurrency.aplwallet.apl.core.exception.AplTransactionExecutionException
 * @since 1.48.4
 */
public class TransactionValidationExceptionMapper implements ExceptionMapper<AplTransactionException> {

    @Override
    public Response toResponse(AplTransactionException exception) {
        return ResponseBuilder.apiError(ApiErrors.EXCEPTION_MESSAGE, exception.getMessage()).build();
    }
}
