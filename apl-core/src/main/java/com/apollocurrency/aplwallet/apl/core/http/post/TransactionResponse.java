package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.json.simple.JSONStreamAware;

@Data
@AllArgsConstructor
public class TransactionResponse {
    private Transaction tx;
    private JSONStreamAware json;
}
