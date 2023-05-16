/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.exception;

import com.apollocurrency.aplwallet.apl.util.builder.ResponseBuilder;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;


/**
 * Generic exception mapper for {@link RestParameterException}.
 *
 * @author isegodin
 */
@Provider
public class RestParameterExceptionMapper implements ExceptionMapper<RestParameterException> {

    @Override
    public Response toResponse(RestParameterException exception) {
        ResponseBuilder responseBuilder = ResponseBuilder.apiError(exception.getApiErrorInfo(), exception.getArgs());
        return responseBuilder.build();
    }

}