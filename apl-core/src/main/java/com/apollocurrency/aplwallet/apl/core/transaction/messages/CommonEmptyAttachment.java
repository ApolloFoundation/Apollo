/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class CommonEmptyAttachment extends EmptyAttachment {

    private final TransactionTypes.TransactionTypeSpec spec;

    public CommonEmptyAttachment(TransactionTypes.TransactionTypeSpec spec) {
        this.spec = spec;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return spec;
    }
}
