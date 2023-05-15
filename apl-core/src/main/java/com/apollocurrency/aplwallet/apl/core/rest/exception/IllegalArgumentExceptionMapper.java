package com.apollocurrency.aplwallet.apl.core.rest.exception;

import com.apollocurrency.aplwallet.apl.util.builder.ResponseBuilder;
import com.apollocurrency.aplwallet.apl.util.exception.ApiErrors;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;


@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        return ResponseBuilder.apiError(ApiErrors.INCORRECT_PARAM_VALUE, exception.getMessage()).build();
    }
}