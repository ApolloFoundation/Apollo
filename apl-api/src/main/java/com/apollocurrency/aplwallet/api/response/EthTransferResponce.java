/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.response;

public class EthTransferResponce {

    private String transactionHash;

    public EthTransferResponce(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }
}
