package com.apollocurrency.aplwallet.vault.model;

public enum KMSResponseStatus {
    NOT_FOUND("Bad credentials"),
    DELETE_ERROR("Internal delete error"),
    DUPLICATE_FOUND("Already exist"),
    BAD_CREDENTIALS("Bad credentials"),
    READ_ERROR("Internal read error"),
    WRITE_ERROR("Internal write error"),
    DECRYPTION_ERROR("Bad credentials"),
    NOT_AVAILABLE("Something went wrong"),
    OK("OK");

    public String message;

    KMSResponseStatus(String message) {
        this.message = message;
    }

    public boolean isOK() {
        return this.message.equals(KMSResponseStatus.OK.message);
    }

    public boolean isDuplicate() {
        return this.message.equals(KMSResponseStatus.DUPLICATE_FOUND.message);
    }
}
