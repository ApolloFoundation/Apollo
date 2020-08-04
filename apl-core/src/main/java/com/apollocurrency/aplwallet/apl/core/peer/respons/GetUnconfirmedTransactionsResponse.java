package com.apollocurrency.aplwallet.apl.core.peer.respons;

import com.apollocurrency.aplwallet.api.p2p.respons.BaseP2PResponse;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import lombok.Getter;

import java.util.List;

@Getter
public class GetUnconfirmedTransactionsResponse extends BaseP2PResponse {
    public List<Transaction> unconfirmedTransactions;

    public GetUnconfirmedTransactionsResponse(List<Transaction> unconfirmedTransactions) {
        this.unconfirmedTransactions = unconfirmedTransactions;
    }
}
