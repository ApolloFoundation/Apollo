package com.apollocurrency.aplwallet.apl.core.rest.exception;

import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<AplException.ValidationException> {
    @Override
    public Response toResponse(AplException.ValidationException exception) {
        JSONObject jsonObject = new JSONObject();
        JSONData.putException(jsonObject, exception);
        return Response.ok().entity(jsonObject.toJSONString()).build();
    }
}
