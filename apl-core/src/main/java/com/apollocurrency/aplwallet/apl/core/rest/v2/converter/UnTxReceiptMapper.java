/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.rest.v2.converter;

import com.apollocurrency.aplwallet.api.v2.model.UnTxReceipt;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.rest.converter.Converter;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import javax.inject.Singleton;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@Singleton
public class UnTxReceiptMapper implements Converter<Transaction, UnTxReceipt> {
    @Override
    public UnTxReceipt apply(Transaction transaction) {
        UnTxReceipt o = new UnTxReceipt();
        o.setTransaction(transaction.getStringId());
        o.setAmount(String.valueOf(transaction.getAmountATM()));
        o.setFee(String.valueOf(transaction.getFeeATM()));
        o.setSender(Convert2.rsAccount(transaction.getSenderId()));
        if(transaction.getRecipientId()!=0) {
            o.setRecipient(Convert2.rsAccount(transaction.getRecipientId()));
        }
        o.setSignature(Convert.toHexString(transaction.getSignature()));
        o.setTimestamp((long) transaction.getTimestamp());
        return o;
    }
}
