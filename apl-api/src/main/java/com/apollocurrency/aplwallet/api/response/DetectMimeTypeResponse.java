package com.apollocurrency.aplwallet.api.response;

public class DetectMimeTypeResponse extends ResponseBase  {

    public String type;

    public DetectMimeTypeResponse(String type) {
        this.type = type;
    }
}
