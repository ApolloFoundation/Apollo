package com.apollocurrency.aplwallet.api.p2p.request;

import java.util.List;
import java.util.UUID;

public class GetTransactionsRequest extends BaseP2PRequest {
    public List<String> transactionIds;

    public GetTransactionsRequest(List<String> transactionIds, UUID chainId) {
        super("getTransactions", chainId);
        this.transactionIds = transactionIds;
    }
}
