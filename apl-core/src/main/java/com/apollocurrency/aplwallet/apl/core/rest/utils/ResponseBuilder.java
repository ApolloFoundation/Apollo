/*
 *
 *  Copyright © 2018-2019 Apollo Foundation
 *
 */

package com.apollocurrency.aplwallet.apl.core.rest.utils;

import com.apollocurrency.aplwallet.api.dto.DTO;
import com.apollocurrency.aplwallet.api.response.ResponseBase;
import com.apollocurrency.aplwallet.apl.core.rest.ErrorInfo;

import javax.ws.rs.core.Response;

public class ResponseBuilder {

    private long startRequestTime;
    protected DTO dto;
    protected ResponseBase response;
    protected int status;

    protected ResponseBuilder(int status) {
        this.status = status;
        this.startRequestTime = System.currentTimeMillis();
    }

    public static Response apiError(int errorNo, String reasonPhrase){
        return apiError(200, errorNo, errorNo, reasonPhrase);
    }

    public static Response apiError(ErrorInfo error){
        String reasonPhrase = Messages.format(error.getErrorDescription());
        return apiError(200, error.getOldErrorCode(), error.getErrorCode(), reasonPhrase);
    }

    public static Response apiError(ErrorInfo error, Object ... args){
        String reasonPhrase = Messages.format(error.getErrorDescription(), args);
        return apiError(200, error.getOldErrorCode(), error.getErrorCode(), reasonPhrase);
    }

    public static Response apiError(int status, int oldErrorNo, int errorNo, String reasonPhrase){
        ResponseBuilder instance = new ResponseBuilder(status);

        ResponseBase body = new ResponseBase();
        body.newErrorCode = errorNo;
        body.errorCode = (long) oldErrorNo;
        body.errorDescription = reasonPhrase;

        instance.response = body;

        return instance.build();
    }

    public static ResponseBuilder ok(){
        return new ResponseBuilder(200);
    }

    public static ResponseBuilder startTiming(){
        return ok();
    }

    public ResponseBuilder error(int status, int errorNo, String reasonPhrase){
        ResponseBase body = new ResponseBase();
        body.errorCode = (long) errorNo;
        body.errorDescription = reasonPhrase;
        this.response = body;
        this.dto = null;
        return this;
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

    public ResponseBuilder bind(DTO dto){
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
        return Response.status(status).entity(entity).build();
    }
}
