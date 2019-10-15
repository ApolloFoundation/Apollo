package com.apollocurrency.aplwallet.apl.core.model;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApprovedAndApprovalTxs {
    /**
     * PhasedTransaction
     */
    private Transaction approved;
    /**
     * Transaction which approving a phased tx.
     */
    private Transaction approval;
}