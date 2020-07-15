package com.apollocurrency.aplwallet.apl.exchange.model;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import lombok.Data;

@Data
public class TransferTransactionInfo {
    /**
     * Transaction in the APL blockchain.
     */
    private Transaction transaction;
    /**
     * Transaction in the ETH blockchain.
     */
    private String txId;

}
