/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.common;

import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import lombok.Getter;

import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class TxBContextImpl extends TxBContext {
    @Getter
    private final Chain chain;

    private TxSerializer txSerializerV1;
    private TxSerializer txSerializerV3;

    protected TxBContextImpl(Chain chain) {
        Objects.requireNonNull(chain);
        this.chain = chain;
    }

    @Override
    public TxSerializer createSerializer(int version) {
        //TODO: check the chain config
        switch (version) {
            case 1:
            case 2:
                if (txSerializerV1 == null) {
                    txSerializerV1 = new TxSerializerV1Impl(this);
                }
                return txSerializerV1;

            case 3:
                if (txSerializerV3 == null) {
                    txSerializerV3 = new TxSerializerV3Impl(this);
                }
                return txSerializerV3;
            default:
                throw new IllegalArgumentException("Illegal transaction version: " + version);
        }
    }
}
