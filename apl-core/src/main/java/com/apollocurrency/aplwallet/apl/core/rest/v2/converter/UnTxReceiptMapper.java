/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.rest.v2.converter;

import com.apollocurrency.aplwallet.api.v2.model.UnTxReceipt;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.rest.converter.Converter;
import com.apollocurrency.aplwallet.apl.core.utils.Convert2;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import javax.inject.Singleton;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@Singleton
public class UnTxReceiptMapper implements Converter<Transaction, UnTxReceipt> {
    @Override
    public UnTxReceipt apply(Transaction model) {
        UnTxReceipt dto = new UnTxReceipt();
        dto.setTransaction(model.getStringId());
        dto.setAmount(String.valueOf(model.getAmountATM()));
        dto.setFee(String.valueOf(model.getFeeATM()));
        dto.setSender(Convert2.rsAccount(model.getSenderId()));
        if (model.getRecipientId() != 0) {
            dto.setRecipient(Convert2.rsAccount(model.getRecipientId()));
        }
        dto.setSignature(Convert.toHexString(model.getSignature().bytes()));
        dto.setTimestamp((long) model.getTimestamp());
        return dto;
    }
}
