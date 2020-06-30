/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.env.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@ToString
public class TransactionFeeSettings {
    @Getter
    private final Map<Short, Short> feeRateMap;

    public TransactionFeeSettings() {
        feeRateMap = null;
    }

    @JsonCreator
    public TransactionFeeSettings(@JsonProperty("feeRates") FeeRate[] feeRates) {
        Objects.requireNonNull(feeRates);

        this.feeRateMap = new HashMap<>();
        for (FeeRate feeRate: feeRates) {
            feeRateMap.put((short) ((feeRate.getType()<<8 | feeRate.getSubType()&0xFF) & 0xFFFF), feeRate.getRate());
        }
    }

    public TransactionFeeSettings(Map<Short, Short> feeRateMap) {
        this.feeRateMap = feeRateMap;
    }

    public short getRate(byte type, byte subType){
        if(feeRateMap != null) {
            short key = (short) ((type << 8 | subType&0xFF) & 0xFFFF);
            return feeRateMap.getOrDefault(key, FeeRate.DEFAULT_RATE);
        }else {
            return FeeRate.DEFAULT_RATE;
        }
    }

    public TransactionFeeSettings copy() {
        return new TransactionFeeSettings(new HashMap<>(feeRateMap));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionFeeSettings that = (TransactionFeeSettings) o;
        return Objects.equals(feeRateMap, that.feeRateMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(feeRateMap);
    }

}
