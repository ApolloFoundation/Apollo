/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.entity.blockchain;

import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import lombok.experimental.Delegate;

import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class UnsignedTransactionWrapper implements Transaction {
    @Delegate(excludes = {ExcludeSignature.class})
    private final Transaction tx;

    public UnsignedTransactionWrapper(Transaction tx) {
        Objects.requireNonNull(tx);
        this.tx = tx;
    }

    @Override
    public Signature getSignature() {
        return null;
    }

    private interface ExcludeSignature {
        Signature getSignature();
    }
}
