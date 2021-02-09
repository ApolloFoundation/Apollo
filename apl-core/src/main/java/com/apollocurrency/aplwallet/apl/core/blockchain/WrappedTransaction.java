/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.blockchain;

import lombok.experimental.Delegate;

import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class WrappedTransaction implements Transaction {
    @Delegate(excludes = {ExcludeTxImpl.class})
    protected final Transaction transaction;

    public WrappedTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public Transaction getTransactionImpl() {
        return transaction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WrappedTransaction)) return false;
        WrappedTransaction that = (WrappedTransaction) o;
        return transaction.equals(that.transaction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transaction);
    }

    private interface ExcludeTxImpl {
        Transaction getTransactionImpl();
    }
}
