/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.TransactionImpl;
import org.json.simple.JSONObject;

import javax.inject.Singleton;

@Singleton
public class TransactionParser {
    private TransactionBuilder builder;
    private TransactionValidator validator;

    public TransactionParser(TransactionBuilder builder, TransactionValidator validator) {
        this.builder = builder;
        this.validator = validator;
    }

    public TransactionImpl parseTransaction(JSONObject transactionData) throws AplException.NotValidException {
        TransactionImpl transaction = builder.newTransactionBuilder(transactionData).build();
        if (transaction.getSignature() != null && !validator.checkSignature(transaction)) {
            throw new AplException.NotValidException("Invalid transaction signature for transaction " + transaction.getJSONObject().toJSONString());
        }
        return transaction;
    }
}
