package com.apollocurrency.aplwallet.api.response;

public class FullHashToIdResponse  extends ResponseBase {

    public String longId;
    public String stringId;

    public FullHashToIdResponse(String longId, String stringId) {
        this.longId = longId;
        this.stringId = stringId;
    }
}
