package com.apollocurrency.aplwallet.api.response;

public class DecodeQRCodeResponse extends ResponseBase {

    public String qrCodeData;

    public DecodeQRCodeResponse(String qrCodeData) {
        this.qrCodeData = qrCodeData;
    }

}
