/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
/*
  Was created to avoid circular dependency on {@link com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator}
 */
public class TransactionValidationHelper {
    private final Blockchain blockchain;

    @Inject
    public TransactionValidationHelper(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    public int getFinishValidationHeight(Transaction transaction, Attachment attachment) {
        return attachment.isPhased(transaction) ? transaction.getPhasing().getFinishHeight() - 1 : blockchain.getHeight();
    }
}
