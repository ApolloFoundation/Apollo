package com.apollocurrency.aplwallet.apl.core.rest.exception;

/**
 * Exception with dedicated mapper {@link RestParameterExceptionMapper}, can be thrown directly from REST endpoint
 *
 * @author isegodin
 */
public class RestParameterException extends RuntimeException {

    /**
     */
    private final Long oldErrorCode;

    /**
     */
    private final Integer errorCode;

    public RestParameterException(Long oldErrorCode, Integer errorCode, String message) {
        super(message);
        this.oldErrorCode = oldErrorCode;
        this.errorCode = errorCode;
    }

    public RestParameterException(Integer errorCode, String message) {
        super(message);
        this.oldErrorCode = null;
        this.errorCode = errorCode;
    }

    public RestParameterException(Long oldErrorCode, Integer errorCode, String message, Throwable cause) {
        super(message, cause);
        this.oldErrorCode = oldErrorCode;
        this.errorCode = errorCode;
    }

    public RestParameterException(Integer errorCode, String message, Throwable cause) {
        super(message, cause);
        this.oldErrorCode = null;
        this.errorCode = errorCode;
    }

    public Long getOldErrorCode() {
        return oldErrorCode;
    }

    public Integer getErrorCode() {
        return errorCode;
    }
}
