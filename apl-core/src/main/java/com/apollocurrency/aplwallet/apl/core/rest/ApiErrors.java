/*
 *
 *  Copyright © 2018-2019 Apollo Foundation
 *
 */

package com.apollocurrency.aplwallet.apl.core.rest;

public enum ApiErrors implements ErrorInfo {

    JSON_SERIALIZATION_EXCEPTION(1,1001,"Exception encountered during generating JSON content, root cause: %s"),

    MISSING_PARAM_LIST(3, 2002, "At least one of [%s] must be specified."),
    MISSING_PARAM(3, 2003, "The mandatory parameter '%s' is not specified."),
    INCORRECT_VALUE(4, 2004, "Incorrect {0} value, '{1}' is not defined"),
    UNKNOWN_VALUE(5, 2005, "Unknown {0} : {1}"),
    PEER_NOT_CONNECTED(5, 2006, "Peer not connected."),
    PEER_NOT_OPEN_API(5, 2007, "Peer is not providing open API."),

    FAILED_TO_ADD(8, 2008, "Failed to add peer %s")
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
