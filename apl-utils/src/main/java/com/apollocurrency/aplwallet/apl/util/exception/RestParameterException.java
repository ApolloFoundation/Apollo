/*
 * Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.exception;

/**
 * Exception with dedicated mapper {@link RestParameterExceptionMapper}, can be thrown directly from REST endpoint
 *
 * @author isegodin
 */
public class RestParameterException extends RuntimeException {

    private final ApiErrorInfo apiErrorInfo;

    private final Object[] args;

    public RestParameterException(Integer oldErrorCode, Integer errorCode, String message) {
        super(message);
        this.args = null;
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
        super(format(apiErrorInfo.getErrorDescription(), args));
        this.apiErrorInfo = apiErrorInfo;
        this.args = args;
    }

    public RestParameterException(Throwable cause, ApiErrorInfo apiErrorInfo, Object... args) {
        super(format(apiErrorInfo.getErrorDescription(), args), cause);
        this.apiErrorInfo = apiErrorInfo;
        this.args = args;
    }

    public ApiErrorInfo getApiErrorInfo() {
        return apiErrorInfo;
    }

    public Object[] getArgs() {
        return args;
    }

    private static String format(String format, Object... args) {
        return Messages.format(format, args);
    }
}
