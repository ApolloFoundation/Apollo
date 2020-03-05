/*
 *
 *  Copyright © 2018-2019 Apollo Foundation
 *
 */

package com.apollocurrency.aplwallet.apl.core.rest.utils;

import com.apollocurrency.aplwallet.api.dto.BaseDTO;
import com.apollocurrency.aplwallet.api.response.ResponseBase;
import com.apollocurrency.aplwallet.api.response.ResponseDone;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.ErrorInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.core.Response;

public class ResponseBuilder {

    private static ObjectMapper mapper = new ObjectMapper();

    private final long startRequestTime;
    protected BaseDTO dto;
    protected ResponseBase response;
    protected int status;

    protected ResponseBuilder(int status) {
        this.status = status;
        this.startRequestTime = System.currentTimeMillis();
    }

    public static ResponseBuilder startTiming(){
        return new ResponseBuilder(200);
    }

    public static ResponseBuilder apiError(ErrorInfo error, Object ... args){
        ResponseBuilder instance = new ResponseBuilder(200);
        instance.error(error, args);
        return instance;
    }

    public static ResponseBuilder detailedApiError(ErrorInfo error, String errorDetails, Object... args) {
        ResponseBuilder instance = new ResponseBuilder(200);
        instance.detailedError(error, errorDetails, args);
        return instance;
    }

    public static ResponseBuilder ok(){
        ResponseBuilder instance = new ResponseBuilder(200);
        instance.response = new ResponseBase(0, null, null, 0L);
        return instance;
    }

    public static ResponseBuilder done(){
        ResponseBuilder instance = new ResponseBuilder(200);
        instance.response = new ResponseDone();
        return instance;
    }

    public static ResponseBase createErrorResponse(ErrorInfo error, String errorDetails, Object... args){
        String reasonPhrase = Messages.format(error.getErrorDescription(), args);
        return  new ResponseBase(error.getErrorCode(), reasonPhrase, errorDetails, (long) error.getOldErrorCode());
    }

    public ResponseBuilder error(ResponseBase errorResponse) {
        this.status = 200;
        this.response = errorResponse;
        this.dto = null;
        return this;
    }

    public ResponseBuilder detailedError(ErrorInfo error, String errorDetails, Object... args) {
        this.status = 200;
        this.response = createErrorResponse(error, errorDetails, args);
        this.dto = null;
        return this;
    }

    public ResponseBuilder error(ErrorInfo error, Object... args) {
        return detailedError(error, null, args);
    }

    public ResponseBuilder status(int status){
        this.status = status;
        return this;
    }

    public ResponseBuilder bind(ResponseBase response){
        this.response = response;
        this.dto = null;
        return this;
    }

    public ResponseBuilder bind(BaseDTO dto){
        this.dto = dto;
        this.response = null;
        return this;
    }

    public Response build(){
        if (dto == null && response == null){
            return Response.status(status).build();
        }
        long elapsed = System.currentTimeMillis() - startRequestTime;
        Object entity;
        if (this.response != null){
            response.requestProcessingTime = elapsed;
            entity = response;
        }else {
            dto.setRequestProcessingTime(elapsed);
            entity=dto;
        }
        String stringJsonEntity;
        try {
            //this conversion is necessary for POST methods to produce TEXT/HTML content
            stringJsonEntity = mapper.writeValueAsString(entity);
        } catch (JsonProcessingException e) {
            ErrorInfo internalError = ApiErrors.JSON_SERIALIZATION_EXCEPTION;
            String reasonPhrase = Messages.format(internalError.getErrorDescription(), e.getMessage());
            ResponseBase body = new ResponseBase(internalError.getErrorCode(), reasonPhrase, (long)internalError.getOldErrorCode());
            return Response.status(500).entity(body).build();
        }
        return Response.status(status).entity(stringJsonEntity).build();
    }
}
