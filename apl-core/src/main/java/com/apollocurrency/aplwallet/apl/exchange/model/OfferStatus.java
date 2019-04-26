package com.apollocurrency.aplwallet.apl.exchange.model;

public enum  OfferStatus {

    OPEN,
    PENDING,
    EXPIRED,
    CANCEL;

    public static OfferStatus getType(int ordinal){
        if(ordinal < 0 || ordinal > OfferStatus.values().length){
            return null;
        }
        return OfferStatus.values()[ordinal];
    }
}
