/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.apl.core.converter.Converter;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.TransactionEntity;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class TransactionModelToEntityConverter implements Converter<Transaction, TransactionEntity> {
    @Override
    public TransactionEntity apply(Transaction transaction) {


        return null;
    }
}
