/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.model;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.util.HexUtils;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

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
        this(address.get());
    }

    public AplAddress(byte[] bytes) {
        if (bytes.length > Long.BYTES) {
            throw new ArithmeticException("AplAddress out of long range");
        }
        this.id = HexUtils.toLong(bytes);
    }

    public static AplAddress valueOf(String address) {
        if (HexUtils.isHex(address)) {
            return new AplAddress(HexUtils.parseHex(address));
        } else {
            return new AplAddress(Convert.parseAccountId(address));
        }
    }

    public long getLongId() {
        return id;
    }

    public String toRS() {
        return Convert2.rsAccount(id);
    }

    @Override
    public byte[] get() {
        return Convert.longToBytes(id);
    }

    @Override
    public String toString() {
        return id + "(" + getHex() + "," + toRS() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AplAddress that = (AplAddress) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
