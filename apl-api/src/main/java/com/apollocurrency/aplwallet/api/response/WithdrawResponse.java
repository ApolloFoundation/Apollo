package com.apollocurrency.aplwallet.api.response;

public class WithdrawResponse {
    public String transactionAddress;

    public WithdrawResponse(String transactionAddress) {
        this.transactionAddress = transactionAddress;
    }
}
