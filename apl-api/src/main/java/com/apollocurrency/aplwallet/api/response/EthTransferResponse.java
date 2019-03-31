/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.response;

public class EthTransferResponse {

    private String transactionHash;

    public EthTransferResponse(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }
}
