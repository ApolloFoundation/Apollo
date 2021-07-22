/*
 * Copyright (c)  2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.apl.core.model.MandatoryTransaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.MandatoryTransactionEntity;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class MandatoryTransactionEntityToModelConverter implements Converter<MandatoryTransactionEntity, MandatoryTransaction> {
    private final TransactionBuilderFactory transactionBuilderFactory;

    @Inject
    public MandatoryTransactionEntityToModelConverter(TransactionBuilderFactory transactionBuilderFactory) {
        this.transactionBuilderFactory = transactionBuilderFactory;
    }

    @Override
    public MandatoryTransaction apply(MandatoryTransactionEntity entity) {
        Objects.requireNonNull(entity);
        try {
            Transaction tx = transactionBuilderFactory.newTransaction(entity.getTransactionBytes());
            MandatoryTransaction model = new MandatoryTransaction(tx, entity.getRequiredTxHash());
            return model;
        } catch (AplException.NotValidException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
