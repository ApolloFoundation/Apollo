/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.model;

import java.util.Objects;

public enum DexCurrency {

    APL("apl"),
    ETH("eth"),
    PAX("pax");

    private String currencyCode;

    DexCurrency(String currency) {
        this.currencyCode = currency;
    }

    public static Integer getValue(DexCurrency i) {
        switch (i) {
            case APL:
                return 0;
            case ETH:
                return 1;
            case PAX:
                return 2;
        }
        return -1;
    }

    public static DexCurrency getType(int ordinal) {
        if (ordinal < 0 || ordinal > DexCurrency.values().length) {
            return null;
        }

        return DexCurrency.values()[ordinal];
    }

    /**
     * Resteasy currency parsing method. Please note, that APL/ETH will be parsed to ETH
     *
     * @param value request parameter in standard format 'apl','eth' or 'APL_ETH','APL/ETH' (end with currency code)
     * @return currency parsed from input
     * @throws IllegalArgumentException when currency was not parsed from input
     */
    public static DexCurrency fromString(String value) {
        Objects.requireNonNull(value, "Dex currency is null");
        for (DexCurrency dexCurrency : values()) {
            if (dexCurrency.currencyCode.equalsIgnoreCase(value) || value.toLowerCase().endsWith(dexCurrency.currencyCode)) {
                return dexCurrency;
            }
        }
        throw new IllegalArgumentException("Incorrect DexCurrency value: " + value);
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public boolean isEthOrPax() {
        return (this == DexCurrency.ETH) || (this == DexCurrency.PAX);
    }

    public boolean isApl() {
        return this == DexCurrency.APL;
    }

    public boolean isEth() {
        return this == DexCurrency.ETH;
    }

    public boolean isPax() {
        return this == DexCurrency.PAX;
    }
}
