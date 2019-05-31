/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.model;

public enum  DexCurrencies {

    APL("apl"),
    ETH("eth"),
    PAX("pax");

    DexCurrencies(String currency) {
        this.currencyCode = currency;
    }

    private String currencyCode;

    public String getCurrencyCode() {
        return currencyCode;
    }

    public static DexCurrencies getType(int ordinal){
        if(ordinal < 0 || ordinal > DexCurrencies.values().length){
            return null;
        }

        return DexCurrencies.values()[ordinal];
    }
}
