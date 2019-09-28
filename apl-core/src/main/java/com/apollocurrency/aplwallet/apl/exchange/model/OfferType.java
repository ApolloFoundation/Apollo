/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.model;

public enum OfferType {

    BUY,
    SELL;


    public static OfferType getType(int ordinal){
        if(ordinal < 0 || ordinal > OfferType.values().length){
            return null;
        }
        return OfferType.values()[ordinal];
    }

    public boolean isBuy(){
        return this.ordinal() == OfferType.BUY.ordinal();
    }

    public boolean isSell(){
        return this.ordinal() == OfferType.SELL.ordinal();
    }

}
