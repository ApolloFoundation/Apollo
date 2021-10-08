/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws;

/**
 * @author andrew.zinchenko@gmail.com
 */
public enum SmcEventServerErrors implements ErrorInfo {
    UNSUPPORTED_OPERATION(1, "Unsupported operation {}"),
    WRONG_REQUEST_STRUCTURE(2, "Wrong request structure."),//can't deserialize request
    INVALID_REQUEST_ARGUMENTS(3, "Invalid request arguments."),//inconsistency parameters
    SUBSCRIPTION_ALREADY_REGISTERED(4, "Subscription is already registered, id={}"),
    ;

    private final int errorCode;
    private final String errorDescription;

    SmcEventServerErrors(int errorCode, String errorDescription) {
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    @Override
    public int getErrorCode() {
        return errorCode;
    }

    @Override
    public String getErrorDescription() {
        return errorDescription;
    }
}
