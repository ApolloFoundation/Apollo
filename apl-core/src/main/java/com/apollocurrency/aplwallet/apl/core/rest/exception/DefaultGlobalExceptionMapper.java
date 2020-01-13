package com.apollocurrency.aplwallet.apl.core.rest.exception;

import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class DefaultGlobalExceptionMapper implements ExceptionMapper<Exception> {
    @Override
    public Response toResponse(Exception exception) {
        String stacktrace = ThreadUtils.getStackTraceSilently(exception);
        return ResponseBuilder.detailedApiError(ApiErrors.UNKNOWN_SERVER_ERROR, stacktrace, exception.getMessage()).build();
    }
}
