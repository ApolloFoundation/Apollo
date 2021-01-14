/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.rest.v2;

import com.apollocurrency.aplwallet.api.v2.model.BaseResponse;
import com.apollocurrency.aplwallet.api.v2.model.ErrorResponse;
import com.apollocurrency.aplwallet.apl.util.exception.ApiErrorInfo;
import com.apollocurrency.aplwallet.apl.util.exception.Messages;

import javax.ws.rs.core.Response;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
public class ResponseBuilderV2 {
    private static final int NO_ERROR_STATUS = 200;
    private static final int ERROR_STATUS = 400;

    private final long startRequestTime;
    protected BaseResponse response;
    protected int status;

    protected ResponseBuilderV2(int status) {
        this.status = status;
        this.startRequestTime = System.currentTimeMillis();
    }

    public static ResponseBuilderV2 startTiming() {
        return new ResponseBuilderV2(NO_ERROR_STATUS);
    }

    public static ResponseBuilderV2 apiError(ApiErrorInfo error, Object... args) {
        ResponseBuilderV2 instance = new ResponseBuilderV2(ERROR_STATUS);
        instance.error(error, args);
        return instance;
    }

    public static ResponseBuilderV2 detailedApiError(ApiErrorInfo error, String errorDetails, Object... args) {
        ResponseBuilderV2 instance = new ResponseBuilderV2(ERROR_STATUS);
        instance.detailedError(error, errorDetails, args);
        return instance;
    }

    public static ResponseBuilderV2 ok() {
        return new ResponseBuilderV2(NO_ERROR_STATUS);
    }

    public static ResponseBuilderV2 done() {
        return ok();
    }

    public static ErrorResponse createErrorResponse(ApiErrorInfo error, String errorDetails, Object... args) {
        //TODO ???
        String reasonPhrase = Messages.format(error.getErrorDescription(), args);
        return new ErrorResponse(error.getErrorCode(), reasonPhrase, errorDetails);
    }

    public ResponseBuilderV2 error(ErrorResponse errorResponse) {
        this.status = 400;
        this.response = errorResponse;
        return this;
    }

    public ResponseBuilderV2 detailedError(ApiErrorInfo error, String errorDetails, Object... args) {
        error(createErrorResponse(error, errorDetails, args));
        return this;
    }

    public ResponseBuilderV2 error(ApiErrorInfo error, Object... args) {
        return detailedError(error, null, args);
    }

    public ResponseBuilderV2 status(int status) {
        this.status = status;
        return this;
    }

    public ResponseBuilderV2 bind(BaseResponse response) {
        this.response = response;
        return this;
    }

    public Response emptyResponse() {
        return this.build();
    }

    public Response build() {
        if ( response == null) {
            this.response = new BaseResponse();
        }
        long elapsed = System.currentTimeMillis() - startRequestTime;
        response.setRequestProcessingTime(elapsed);
        return Response.status(status).entity(response).build();
    }
}
