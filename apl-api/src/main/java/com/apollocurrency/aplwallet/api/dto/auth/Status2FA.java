package com.apollocurrency.aplwallet.api.dto.auth;

/**
 * @author al
 */
public enum Status2FA {
    OK,
    NOT_FOUND,
    ALREADY_CONFIRMED,
    INCORRECT_CODE,
    INTERNAL_ERROR,
    NOT_ENABLED,
    ALREADY_ENABLED,
    NOT_CONFIRMED;

    public boolean isOK() {
        return this.equals(Status2FA.OK);
    }
}
