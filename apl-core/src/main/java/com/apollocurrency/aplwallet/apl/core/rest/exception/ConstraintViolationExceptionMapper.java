package com.apollocurrency.aplwallet.apl.core.rest.exception;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.util.Iterator;

import com.apollocurrency.aplwallet.api.response.ResponseBase;
import org.jboss.resteasy.api.validation.ResteasyViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic exception mapper for resteasy validation-provider.
 *
 * @author isegodin
 */
@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ResteasyViolationException> {

    private static final Logger logger = LoggerFactory.getLogger(ConstraintViolationExceptionMapper.class);

    @SuppressWarnings("unchecked")
    @Override
    public Response toResponse(ResteasyViolationException exception) {
        ConstraintViolation<?> violation = exception.getConstraintViolations().iterator().next();
        ConstraintDescriptor<Annotation> constraintDescriptor = (ConstraintDescriptor<Annotation>) violation.getConstraintDescriptor();

        ResponseBase responseEntity = new ResponseBase();

        Object oldErrorCode = constraintDescriptor.getAttributes().get("oldErrorCode");
        if (!(oldErrorCode instanceof Long)) {
            logger.error("Rest validation annotation of type {} should have field 'long oldErrorCode'.", constraintDescriptor.getAnnotation().annotationType());
        } else {
            Long code = (Long) oldErrorCode;
            if (code > 0) {
//                responseEntity.setErrorCode(code);
            }
        }

        Object errorCode = constraintDescriptor.getAttributes().get("errorCode");
        if (!(errorCode instanceof Integer)) {
            logger.error("Rest validation annotation of type {} should have field 'int errorCode'.", constraintDescriptor.getAnnotation().annotationType());
        } else {
            Integer code = (Integer) errorCode;
            if (code > 0) {
//                responseEntity.setNewErrorCode(code);
            }
        }

//        responseEntity.setErrorDescription(buildMessage(violation));

        return Response.status(Response.Status.OK)
                .entity(responseEntity)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private String buildMessage(ConstraintViolation<?> violation) {
        String name = null;
        for (Iterator<Path.Node> iterator = violation.getPropertyPath().iterator(); iterator.hasNext(); ) {
            name = iterator.next().getName();
        }
        if (name != null) {
            return name + ": " + violation.getMessage();
        }
        return violation.getMessage();
    }

}