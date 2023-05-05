/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.exception;

import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import com.apollocurrency.aplwallet.apl.util.builder.ResponseBuilder;
import com.apollocurrency.aplwallet.apl.util.exception.ApiErrors;
import lombok.extern.slf4j.Slf4j;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Slf4j
@Provider
public class DefaultGlobalExceptionMapper implements ExceptionMapper<Exception> {
    @Override
    public Response toResponse(Exception exception) {
        String stacktrace = ThreadUtils.getStackTraceSilently(exception);
        String message = exception.getMessage();
        log.debug("GlobalErrorHandler error = {}", message);
        if (exception instanceof jakarta.ws.rs.NotFoundException) {
            log.warn("REST API NotFoundException", exception);
            return ResponseBuilder.apiError(ApiErrors.REST_API_SERVER_ERROR, message).build();
        }
        if (exception instanceof com.fasterxml.jackson.core.JsonParseException) {
            return ResponseBuilder.apiError(ApiErrors.REST_API_SERVER_ERROR, exception.getClass().getSimpleName() + ": " + message).build();
        }
        log.debug("GlobalErrorHandler stacktrace = {}", stacktrace);
        return ResponseBuilder.detailedApiError(ApiErrors.UNKNOWN_SERVER_ERROR, stacktrace, message).build();
    }
}
