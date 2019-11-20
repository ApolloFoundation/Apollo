package com.apollocurrency.aplwallet.apl.core.rest.exception;

import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class DefaultGlobalExceptionMapper implements ExceptionMapper<Exception> {
    @Override
    public Response toResponse(Exception exception) {
        String stacktrace;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             PrintWriter printWriter = new PrintWriter(out)) {
            exception.printStackTrace(printWriter);
            printWriter.flush();
            byte[] bytes = out.toByteArray();
            stacktrace = new String(bytes);
        } catch (IOException e) {
            return ResponseBuilder.detailedApiError(ApiErrors.UNKNOWN_SERVER_ERROR, e.getMessage(), "(unable to extract stacktrace)").build();
        }
        return ResponseBuilder.detailedApiError(ApiErrors.UNKNOWN_SERVER_ERROR, stacktrace, exception.getMessage()).build();
    }
}
