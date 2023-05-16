/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;

import jakarta.enterprise.inject.Vetoed;

@Vetoed
public class OrdinaryPaymentAttachment extends EmptyAttachment {

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.ORDINARY_PAYMENT;
    }

}
