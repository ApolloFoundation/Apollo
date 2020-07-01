/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.env.config;

import com.apollocurrency.aplwallet.apl.util.annotation.FeeMarker;
import com.apollocurrency.aplwallet.apl.util.annotation.TransactionFee;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.Getter;
import lombok.ToString;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@ToString
@JsonSerialize(using = TransactionFeeSettings.FeeMapSerializer.class)
public class TransactionFeeSettings {
    @Getter
    @JsonIgnore
    private final Map<Short, Short> feeRateMap;

    public TransactionFeeSettings() {
        this(new HashMap<>());
    }

    @JsonCreator
    public TransactionFeeSettings(@JsonProperty("feeRates") FeeRate[] feeRates) {
        this();
        Objects.requireNonNull(feeRates);
        for (FeeRate feeRate: feeRates) {
            feeRateMap.put(FeeRate.createKey(feeRate.getType(), feeRate.getSubType()), feeRate.getRate());
        }
    }

    public TransactionFeeSettings(Map<Short, Short> feeRateMap) {
        this.feeRateMap = feeRateMap;
    }

    @TransactionFee(FeeMarker.FEE_RATE)
    public short getRate(byte type, byte subType) {
        if (feeRateMap.isEmpty()) {
            return FeeRate.DEFAULT_RATE;
        } else {
            short key = FeeRate.createKey(type, subType);
            return feeRateMap.getOrDefault(key, FeeRate.DEFAULT_RATE);
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

    static class FeeMapSerializer extends StdSerializer<TransactionFeeSettings> {

        public FeeMapSerializer() {
            this(null);
        }

        public FeeMapSerializer(Class<TransactionFeeSettings> t) {
            super(t);
        }

        @Override
        public void serialize(TransactionFeeSettings value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeArrayFieldStart("feeRates");
            if(value.getFeeRateMap() != null) {
                for (Map.Entry<Short, Short> entry : value.getFeeRateMap().entrySet()) {
                    gen.writeObject(new FeeRate(entry.getKey(), entry.getValue()));
                }
            }
            gen.writeEndArray();
            gen.writeEndObject();

        }
    }

}
