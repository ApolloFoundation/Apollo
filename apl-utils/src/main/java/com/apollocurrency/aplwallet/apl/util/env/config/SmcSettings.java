/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.env.config;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Getter
@EqualsAndHashCode
public class SmcSettings {
    private static final long DEFAULT_FUEL_LIMIT_MIN_VALUE = 10000000L;
    private static final long DEFAULT_FUEL_LIMIT_MAX_VALUE = 30000000000L;
    private static final long DEFAULT_FUEL_PRICE_MIN_VALUE = 10000L;
    private static final long DEFAULT_FUEL_PRICE_MAX_VALUE = 100000000L;

    private final String masterAccountPublicKey;
    @JsonIgnore
    private final byte[] masterAccountPK;
    private final long fuelLimitMinValue;
    private final long fuelLimitMaxValue;
    private final long fuelPriceMinValue;
    private final long fuelPriceMaxValue;

    @JsonCreator
    public SmcSettings(@JsonProperty(value = "masterAccountPublicKey", required = true) String masterAccountPublicKey,
                       @JsonProperty("fuelLimitMinValue") long fuelLimitMinValue,
                       @JsonProperty("fuelLimitMaxValue") long fuelLimitMaxValue,
                       @JsonProperty("fuelPriceMinValue") long fuelPriceMinValue,
                       @JsonProperty("fuelPriceMaxValue") long fuelPriceMaxValue) {
        this(Convert.parseHexString(masterAccountPublicKey), fuelLimitMinValue, fuelLimitMaxValue, fuelPriceMinValue, fuelPriceMaxValue);
    }

    public SmcSettings(byte[] masterAccountPK, long fuelLimitMinValue, long fuelLimitMaxValue, long fuelPriceMinValue, long fuelPriceMaxValue) {
        if (masterAccountPK == null) {
            System.out.println("The feature 'Publish smart-contract transaction' will be disabled for this instance of settings, cause masterAccountPublicKey is null.");
            this.masterAccountPublicKey = "";
        } else {
            this.masterAccountPublicKey = Convert.toHexString(masterAccountPK);
        }
        this.masterAccountPK = masterAccountPK;
        this.fuelLimitMinValue = fuelLimitMinValue;
        this.fuelLimitMaxValue = fuelLimitMaxValue;
        if (fuelLimitMaxValue < fuelLimitMinValue) {
            throw new IllegalArgumentException("Invalid fuel limit Min/Max value");
        }
        this.fuelPriceMinValue = fuelPriceMinValue;
        this.fuelPriceMaxValue = fuelPriceMaxValue;
        if (fuelPriceMaxValue < fuelPriceMinValue) {
            throw new IllegalArgumentException("Invalid fuel price Min/Max value");
        }
    }

    public SmcSettings() {
        this((byte[]) null,
            DEFAULT_FUEL_LIMIT_MIN_VALUE, DEFAULT_FUEL_LIMIT_MAX_VALUE,
            DEFAULT_FUEL_PRICE_MIN_VALUE, DEFAULT_FUEL_PRICE_MAX_VALUE);
    }

    public SmcSettings(byte[] pk) {
        this(pk,
            DEFAULT_FUEL_LIMIT_MIN_VALUE, DEFAULT_FUEL_LIMIT_MAX_VALUE,
            DEFAULT_FUEL_PRICE_MIN_VALUE, DEFAULT_FUEL_PRICE_MAX_VALUE);
    }

    public SmcSettings(String pk) {
        this(pk,
            DEFAULT_FUEL_LIMIT_MIN_VALUE, DEFAULT_FUEL_LIMIT_MAX_VALUE,
            DEFAULT_FUEL_PRICE_MIN_VALUE, DEFAULT_FUEL_PRICE_MAX_VALUE);
    }

    public SmcSettings copy() {
        return new SmcSettings(masterAccountPublicKey, fuelLimitMinValue, fuelLimitMaxValue, fuelPriceMinValue, fuelPriceMaxValue);
    }

    @Override
    public String toString() {
        return "SmcSettings{" +
            "masterAccountPublicKey=" + masterAccountPublicKey +
            ", fuelLimitMinValue=" + fuelLimitMinValue +
            ", fuelLimitMaxValue=" + fuelLimitMaxValue +
            ", fuelPriceMinValue=" + fuelPriceMinValue +
            ", fuelPriceMaxValue=" + fuelPriceMaxValue +
            '}';
    }
}
