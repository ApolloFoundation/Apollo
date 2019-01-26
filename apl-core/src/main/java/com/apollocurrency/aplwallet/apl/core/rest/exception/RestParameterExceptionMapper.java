package com.apollocurrency.aplwallet.apl.core.rest.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.apollocurrency.aplwallet.api.response.ResponseBase;


/**
 * Generic exception mapper for {@link RestParameterException}.
 *
 * @author isegodin
 */
@Provider
public class RestParameterExceptionMapper implements ExceptionMapper<RestParameterException> {

    @SuppressWarnings("unchecked")
    @Override
    public Response toResponse(RestParameterException exception) {
        ResponseBase responseEntity = new ResponseBase();
//        responseEntity.setErrorDescription(exception.getMessage());
//        responseEntity.setErrorCode(exception.getOldErrorCode());
//        responseEntity.setNewErrorCode(exception.getErrorCode());

        return Response.status(Response.Status.OK)
                .entity(responseEntity)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

}