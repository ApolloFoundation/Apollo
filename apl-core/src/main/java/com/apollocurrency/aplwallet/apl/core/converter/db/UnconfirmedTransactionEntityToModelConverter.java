/*
 * Copyright (c)  2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransactionEntity;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionUtils;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class UnconfirmedTransactionEntityToModelConverter implements Converter<UnconfirmedTransactionEntity, UnconfirmedTransaction> {
    private final TransactionBuilderFactory transactionBuilderFactory;

    @Inject
    public UnconfirmedTransactionEntityToModelConverter(TransactionBuilderFactory transactionBuilderFactory) {
        this.transactionBuilderFactory = transactionBuilderFactory;
    }

    @Override
    public UnconfirmedTransaction apply(UnconfirmedTransactionEntity entity) {
        Objects.requireNonNull(entity);
        try {
            JSONObject prunableAttachments = null;
            if (entity.getPrunableAttachmentJsonString() != null) {
                prunableAttachments = (JSONObject) JSONValue.parse(entity.getPrunableAttachmentJsonString());
            }
            Transaction tx = transactionBuilderFactory.newTransaction(entity.getTransactionBytes(), prunableAttachments);
            tx.setHeight(entity.getHeight());

            return new UnconfirmedTransaction(tx, entity.getArrivalTimestamp(), entity.getFeePerByte(), TransactionUtils.calculateFullSize(tx, entity.getTransactionBytes().length));
        } catch (AplException.NotValidException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
