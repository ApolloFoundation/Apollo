package com.apollocurrency.aplwallet.apl.core.rest.exception;

import com.apollocurrency.aplwallet.apl.core.rest.utils.ResponseBuilder;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static com.apollocurrency.aplwallet.apl.core.rest.ApiErrors.CONSTRAINT_VIOLATION;

/**
 * Generic exception mapper for resteasy validation-provider.
 *
 * @author isegodin
 */
@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        StringBuilder errorDescription = new StringBuilder();
        for (ConstraintViolation<?> viol : exception.getConstraintViolations()) {
            String message = viol.getMessage();
            String parameter = viol.getPropertyPath().toString();
            errorDescription.append(String.format("%s %s, got value [%s];", parameter, message, viol.getInvalidValue()));
        }
        return ResponseBuilder.apiError(CONSTRAINT_VIOLATION, errorDescription).build();
    }
}