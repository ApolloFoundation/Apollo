/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.exception;

import com.apollocurrency.aplwallet.apl.util.builder.ResponseBuilder;
import com.apollocurrency.aplwallet.apl.util.exception.ApiErrors;
import com.apollocurrency.aplwallet.apl.util.exception.RestParameterException;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;


/**
 * Global handler for {@link ClientErrorException} thrown from REST endpoints
 *
 * @author isegodin
 */
@Provider
public class ClientErrorExceptionMapper implements ExceptionMapper<ClientErrorException> {

    @Override
    public Response toResponse(ClientErrorException exception) {
        ResponseBuilder responseBuilder;
        if (exception instanceof NotFoundException && exception.getCause() instanceof NumberFormatException) {
            responseBuilder = ResponseBuilder.apiError(
                ApiErrors.INCORRECT_PARAM_VALUE, "number format " + exception.getCause().getMessage());
        } else if (exception.getCause() != null && exception.getCause() instanceof RestParameterException) {
            RestParameterException cause = (RestParameterException) exception.getCause();
            responseBuilder = ResponseBuilder.apiError(cause.getApiErrorInfo(), cause.getArgs());
        } else {
            responseBuilder = ResponseBuilder.apiError(
                ApiErrors.INTERNAL_SERVER_EXCEPTION, exception.getMessage());
        }
        return responseBuilder.build();
    }

}