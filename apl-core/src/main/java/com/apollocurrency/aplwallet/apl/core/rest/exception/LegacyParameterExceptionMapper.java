package com.apollocurrency.aplwallet.apl.core.rest.exception;

import com.apollocurrency.aplwallet.api.response.ResponseBase;
import com.apollocurrency.aplwallet.apl.util.exception.ParameterException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.StringWriter;

@Provider
public class LegacyParameterExceptionMapper implements ExceptionMapper<ParameterException> {

    @Override
    public Response toResponse(ParameterException exception) {
        Object responseEntity;
        if (exception.getErrorResponse() != null) {
            try {
                StringWriter writer = new StringWriter();
                exception.getErrorResponse().writeJSONString(writer);

                responseEntity = writer.toString();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            ResponseBase responseBase = new ResponseBase();
//            responseBase.setErrorCode((long) Response.Status.BAD_REQUEST.getStatusCode());
//            responseBase.setErrorDescription(exception.getMessage());

            responseEntity = responseBase;
        }

        return Response.status(Response.Status.OK)
            .entity(responseEntity)
            .type(MediaType.APPLICATION_JSON)
            .build();
    }
}
