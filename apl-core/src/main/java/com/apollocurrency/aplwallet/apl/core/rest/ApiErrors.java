/*
 *
 *  Copyright © 2018-2019 Apollo Foundation
 *
 */

package com.apollocurrency.aplwallet.apl.core.rest;

public enum ApiErrors implements ErrorInfo {

    INCORRECT_VALUE(4,1004,"Incorrect {0} value, '{1}' is not defined"),
    UNKNOWN_VALUE(5,1005,"Unknown {0} : {1}")
    ;



    private int oldErrorCode;
    private int errorCode;
    private String errorDescription;

    ApiErrors(int oldErrorCode, int errorCode, String errorDescription) {
        this.oldErrorCode = oldErrorCode;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

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
        return errorDescription;
    }
}
