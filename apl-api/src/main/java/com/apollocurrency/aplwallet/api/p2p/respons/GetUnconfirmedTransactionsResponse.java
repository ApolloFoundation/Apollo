package com.apollocurrency.aplwallet.api.p2p.respons;

import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import lombok.Getter;

import java.util.List;

@Getter
public class GetUnconfirmedTransactionsResponse extends BaseP2PResponse {
    public List<TransactionDTO> unconfirmedTransactions;
}
