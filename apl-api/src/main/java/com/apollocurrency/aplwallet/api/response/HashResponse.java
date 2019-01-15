package com.apollocurrency.aplwallet.api.response;

public class HashResponse  extends ResponseBase {

    public String hash;

    public HashResponse(String hash) {
        this.hash = hash;
    }
}