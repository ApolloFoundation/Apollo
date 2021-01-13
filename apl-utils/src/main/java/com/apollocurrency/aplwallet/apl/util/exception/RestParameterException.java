/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.exception;

/**
 * Exception with dedicated mapper {@link RestParameterExceptionMapper}, can be thrown directly from REST endpoint
 *
 * @author isegodin
 */
public class RestParameterException extends RuntimeException {

    /**
     *
     */
    private ApiErrorInfo apiErrorInfo;

    /**
     *
     */
    private Object[] args;

    public RestParameterException(Integer oldErrorCode, Integer errorCode, String message) {
        super(message);
        this.apiErrorInfo = new ApiErrorInfo() {
            @Override
            public int getErrorCode() {
                return errorCode;
            }

            @Override
            public int getOldErrorCode() {
                return oldErrorCode;
            }

            @Override
            public String getErrorDescription() {
                return message;
            }
        };
    }

    public RestParameterException(ApiErrorInfo apiErrorInfo, Object... args) {
        super(apiErrorInfo.getErrorDescription());
        this.apiErrorInfo = apiErrorInfo;
        this.args = args;
    }

    public RestParameterException(Throwable cause, ApiErrorInfo apiErrorInfo, Object... args) {
        super(apiErrorInfo.getErrorDescription(), cause);
        this.apiErrorInfo = apiErrorInfo;
        this.args = args;
    }

    public ApiErrorInfo getApiErrorInfo() {
        return apiErrorInfo;
    }

    public Object[] getArgs() {
        return args;
    }
}
