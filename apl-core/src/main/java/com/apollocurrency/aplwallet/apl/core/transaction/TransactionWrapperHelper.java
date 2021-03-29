/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.blockchain.WrappedTransaction;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author andrew.zinchenko@gmail.com
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class TransactionWrapperHelper {

    public static Transaction createUnsignedTransaction(Transaction tx) {
        return new UnsignedTransaction(tx);
    }

    private static class UnsignedTransaction extends WrappedTransaction {

        UnsignedTransaction(Transaction tx) {
            super(tx);
        }

        @Override
        public Signature getSignature() {
            return null;
        }

    }
}
