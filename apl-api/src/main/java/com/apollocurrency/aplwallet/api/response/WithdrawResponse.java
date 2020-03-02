package com.apollocurrency.aplwallet.api.response;

public class WithdrawResponse extends ResponseBase {
    public String transactionAddress;

    public WithdrawResponse() {
    }

    public WithdrawResponse(String transactionAddress) {
        this.transactionAddress = transactionAddress;
    }
}
