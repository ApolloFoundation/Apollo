/*
 *
 *  Copyright © 2018-2019 Apollo Foundation
 *
 */

package com.apollocurrency.aplwallet.apl.core.rest;

public enum ApiErrors implements ErrorInfo {

    INTERNAL_SERVER_EXCEPTION(0,1000,"Internal error, root cause: %s"),
    JSON_SERIALIZATION_EXCEPTION(1,1001,"Exception encountered during generating JSON content, root cause: %s"),

    MISSING_PARAM_LIST(3, 2002, "At least one of [%s] must be specified."),
    MISSING_PARAM(3, 2003, "The mandatory parameter '%s' is not specified."),
    INCORRECT_VALUE(4, 2004, "Incorrect {0} value, [{1}] is not defined or wrong"),
    UNKNOWN_VALUE(5, 2005, "Unknown {0} : {1}"),
    OUT_OF_RANGE(4, 2009, "{0} is not in range [{1}..{2}]"),
    PEER_NOT_CONNECTED(5, 2006, "Peer not connected."),
    PEER_NOT_OPEN_API(5, 2007, "Peer is not providing open API."),
    FAILED_TO_ADD(8, 2008, "Failed to add peer %s"),
    ACCOUNT_GENERATION_ERROR(6, 2009, "Error occurred during account generation."),
    ONLY_ONE_OF_PARAM_LIST(6, 2010, "Not more than one of [%s] can be specified."),
    INCORRECT_PARAM_VALUE(4, 2011, "Incorrect %s"),
    ACCOUNT_2FA_ERROR(22, 2012, "%s"),
    NO_PASSWORD_IN_CONFIG(8, 2010, "Administrator's password is not configured. Please set apl.adminPassword"),
    INCORRECT_PARAM(4, 2011, "Incorrect {0}, {1}"),
    UNKNOWN_SERVER_ERROR(1, 2012, "Unknown server error: '%s', see stacktrace for details"),
    WEB3J_CRYPTO_ERROR(-1, 2013, "Web3j crypto error: '%s', see stacktrace for details"),
    ETH_NODE_ERROR(-1, 2014, "Unable to query eth node: '%s'"),
    PARAM_GREATER_OR_EQUAL_ERROR(4, 2015, "''{0}'' is greater or equal to ''{1}''"),

    DEX_NOT_ENOUGH_AMOUNT(6, 2016, "Not enough %s"),
    DEX_NOT_ENOUGH_FEE(6, 2016, "Not enough {0} for a fee. Min value is {1} {0}")
    ;
    public static final Long CONSTRAINT_VIOLATION_ERROR_CODE = 2014L;

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
