package com.apollocurrency.aplwallet.api.p2p.request;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class GetUnconfirmedTransactionsRequest extends BaseP2PRequest {

    public List<String> exclude = new ArrayList<>();

    public GetUnconfirmedTransactionsRequest(UUID chainId) {
        super("getUnconfirmedTransactions", chainId);
    }
}
