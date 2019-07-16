/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.exception;

import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;


/**
 * Generic exception mapper for {@link RestParameterException}.
 *
 * @author isegodin
 */
@Provider
public class RestParameterExceptionMapper implements ExceptionMapper<RestParameterException> {

    @Override
    public Response toResponse(RestParameterException exception) {
        ResponseBuilder responseBuilder = ResponseBuilder.apiError(exception.getErrorInfo(), exception.getArgs());
        return responseBuilder.build();
    }

}