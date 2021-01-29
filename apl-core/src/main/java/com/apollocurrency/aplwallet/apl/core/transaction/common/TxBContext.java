/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.common;

import com.apollocurrency.aplwallet.apl.util.env.config.Chain;

/**
 * @author andrew.zinchenko@gmail.com
 */
public abstract class TxBContext {

    protected TxBContext() {
    }

    public static TxBContext newInstance(Chain chain) {
        return new TxBContextImpl(chain);
    }

    public abstract TxSerializer createSerializer(int version);

}
