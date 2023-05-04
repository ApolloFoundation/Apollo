/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.exception;

import com.apollocurrency.aplwallet.apl.core.exception.AplCoreLogicException;
import com.apollocurrency.aplwallet.apl.util.builder.ResponseBuilder;
import com.apollocurrency.aplwallet.apl.util.exception.ApiErrors;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Default rest API handler for all apl core exceptions, which are not handled by other more specific handlers
 * @author Andrii Boiarskyi
 * @see AplCoreLogicException
 * @see TransactionValidationExceptionMapper
 * @since 1.48.4
 */
@Provider
public class AplCoreLogicExceptionMapper implements ExceptionMapper<AplCoreLogicException> {

@Override
public Response toResponse(AplCoreLogicException exception) {
    return ResponseBuilder.apiError(ApiErrors.EXCEPTION_MESSAGE, exception.getMessage()).build();
    }
}
