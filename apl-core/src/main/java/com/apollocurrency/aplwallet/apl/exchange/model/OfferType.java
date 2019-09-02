/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.model;

public enum OfferType {

    BUY,
    SELL;


    public static OfferType getType(int ordinal){
        if(ordinal < 0 || ordinal > OfferType.values().length){
            throw new IllegalArgumentException("Offer type with order: " + ordinal + " doesn't exist.");
        }
        return OfferType.values()[ordinal];
    }

    public boolean isBuy(){
        return this.ordinal() == OfferType.BUY.ordinal();
    }

    public boolean isSell(){
        return this.ordinal() == OfferType.SELL.ordinal();
    }

    public OfferType reverse(){
        return isBuy() ? OfferType.SELL : OfferType.BUY;
    }

}
