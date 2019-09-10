package com.apollocurrency.aplwallet.apl.exchange.model;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import lombok.Data;

@Data
public class TransferTransactionInfo {
    private Transaction transaction;
    private String txId;

}
