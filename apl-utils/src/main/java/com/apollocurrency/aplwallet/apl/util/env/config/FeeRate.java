/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.env.config;

import com.apollocurrency.aplwallet.apl.util.annotation.FeeMarker;
import com.apollocurrency.aplwallet.apl.util.annotation.TransactionFee;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Optional;

@TransactionFee(FeeMarker.FEE_RATE)
@EqualsAndHashCode
@ToString
public class FeeRate {
    public static final short RATE_DIVIDER = 100;
    public static final int DEFAULT_RATE = 100; // in percent, that equals to multiply by 1
    @Getter
    private final byte type;
    @Getter
    private final byte subType;
    @Getter
    private final int rate;
    @JsonProperty("baseFee")
    private final BigDecimal baseFee;
    @JsonProperty("sizeFee")
    private final BigDecimal sizeFee;

    @Getter

    @JsonProperty("additionalFees")
    private final BigDecimal[] additionalFees;

    @JsonCreator
    public FeeRate(@JsonProperty(value = "type", required = true) byte type,
                   @JsonProperty(value = "subType", required = true) byte subType,
                   @JsonProperty("rate") Integer rate,
                   @JsonProperty("baseFee") BigDecimal baseFee,
                   @JsonProperty("sizeFee") BigDecimal sizeFee,
                   @JsonProperty("additionalFees") BigDecimal[] additionalFees) {
        this.type = type;
        this.subType = subType;
        if (rate == null) {
            rate = DEFAULT_RATE;
        } else if (rate < 0) {
            throw new IllegalArgumentException("Wrong rate value.");
        }
        this.rate = rate;
        this.baseFee =  baseFee;
        this.sizeFee =  sizeFee;
        this.additionalFees = additionalFees == null ? new BigDecimal[0] : additionalFees;
    }

    @JsonIgnore
    public Optional<BigDecimal> getBaseFee() {
        return Optional.ofNullable(baseFee);
    }

    @JsonIgnore // ignore optionals
    public Optional<BigDecimal> getSizeFee() {
        return Optional.ofNullable(sizeFee);
    }

    public FeeRate copy() {
        return new FeeRate(type, subType, rate, baseFee, sizeFee, additionalFees);
    }

    public static short createKey(short type, short subType){
        return (short) ((type << 8 | subType&0xFF) & 0xFFFF);
    }
}
