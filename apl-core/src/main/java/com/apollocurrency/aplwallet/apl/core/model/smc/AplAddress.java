/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.model.smc;

import com.apollocurrency.smc.data.type.Address;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigInteger;

/**
 * Smart contract address
 *
 * @author andrew.zinchenko@gmail.com
 */
public class AplAddress implements Address {
    @JsonProperty
    long id;

    public AplAddress(@JsonProperty long id) {
        this.id = id;
    }

    public AplAddress(Address address) {
        this.id = new BigInteger(address.get()).longValueExact();
    }

    public long getLongId() {
        return id;
    }

    @Override
    public byte[] get() {
        return BigInteger.valueOf(id).toByteArray();
    }

    @Override
    public String toString() {
        return id + "(" + getHex() + ")";
    }
}
