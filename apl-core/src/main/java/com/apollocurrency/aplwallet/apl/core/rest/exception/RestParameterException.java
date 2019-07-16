/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.exception;

import com.apollocurrency.aplwallet.apl.core.rest.ErrorInfo;

/**
 * Exception with dedicated mapper {@link RestParameterExceptionMapper}, can be thrown directly from REST endpoint
 *
 * @author isegodin
 */
public class RestParameterException extends RuntimeException {

    /**
     */
    private ErrorInfo errorInfo;

    /**
     */
    private Object[] args;

    public RestParameterException(Integer oldErrorCode, Integer errorCode, String message) {
        super(message);
        this.errorInfo = new ErrorInfo() {
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

    public RestParameterException(ErrorInfo errorInfo, Object ... args) {
        super(errorInfo.getErrorDescription());
        this.errorInfo = errorInfo;
        this.args = args;
    }

    public RestParameterException(Throwable cause, ErrorInfo errorInfo, Object ... args) {
        super(errorInfo.getErrorDescription(), cause);
        this.errorInfo = errorInfo;
        this.args = args;
    }

    public ErrorInfo getErrorInfo() {
        return errorInfo;
    }

    public Object[] getArgs() {
        return args;
    }
}
