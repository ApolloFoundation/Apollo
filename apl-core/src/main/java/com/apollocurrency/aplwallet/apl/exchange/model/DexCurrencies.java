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
    
    public static Integer getValue( DexCurrencies i ) {
        switch (i) {
            case APL : return 0;
            case ETH : return 1;
            case PAX : return 2; 
        }
        return -1;
    }

    public static DexCurrencies getType(int ordinal){
        if(ordinal < 0 || ordinal > DexCurrencies.values().length){
            return null;
        }

        return DexCurrencies.values()[ordinal];
    }

    public boolean isEthOrPax(){
        return (this == DexCurrencies.ETH) || (this == DexCurrencies.PAX);
    }

    public boolean isApl(){
        return this == DexCurrencies.APL;
    }
    public boolean isEth(){
        return this == DexCurrencies.ETH;
    }
    public boolean isPax(){
        return this == DexCurrencies.PAX;
    }
}
