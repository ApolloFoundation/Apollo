/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.model;

public enum DexCurrency {

    APL("apl"),
    ETH("eth"),
    PAX("pax");

    DexCurrency(String currency) {
        this.currencyCode = currency;
    }

    private String currencyCode;

    public String getCurrencyCode() {
        return currencyCode;
    }

    public static Integer getValue( DexCurrency i ) {
        switch (i) {
            case APL : return 0;
            case ETH : return 1;
            case PAX : return 2;
        }
        return -1;
    }

    public static DexCurrency getType(int ordinal){
        if(ordinal < 0 || ordinal > DexCurrency.values().length){
            return null;
        }

        return DexCurrency.values()[ordinal];
    }

    public boolean isEthOrPax(){
        return (this == DexCurrency.ETH) || (this == DexCurrency.PAX);
    }

    public boolean isApl(){
        return this == DexCurrency.APL;
    }
    public boolean isEth(){
        return this == DexCurrency.ETH;
    }
    public boolean isPax(){
        return this == DexCurrency.PAX;
    }

    public static DexCurrency fromString(String value) {
        for (DexCurrency dexCurrency : values()) {
            if (dexCurrency.currencyCode.equalsIgnoreCase(value) || value.toLowerCase().endsWith(dexCurrency.currencyCode)) {
                return dexCurrency;
            }
        }
        throw new IllegalArgumentException("Incorrect DexCurrency value: " + value);
    }
}
