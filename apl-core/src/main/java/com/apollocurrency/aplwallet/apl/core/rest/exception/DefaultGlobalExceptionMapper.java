/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.exception;

import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Slf4j
@Provider
public class DefaultGlobalExceptionMapper implements ExceptionMapper<Exception> {
    @Override
    public Response toResponse(Exception exception) {
        String stacktrace = ThreadUtils.getStackTraceSilently(exception);
        if (exception instanceof javax.ws.rs.NotFoundException) {
            log.warn("REST API NotFoundException", exception);
            return ResponseBuilder.apiError(ApiErrors.REST_API_SERVER_ERROR, exception.getMessage()).build();
        }
        return ResponseBuilder.detailedApiError(ApiErrors.UNKNOWN_SERVER_ERROR, stacktrace, exception.getMessage()).build();
    }
}
