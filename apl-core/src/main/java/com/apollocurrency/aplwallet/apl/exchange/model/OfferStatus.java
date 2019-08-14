package com.apollocurrency.aplwallet.apl.exchange.model;

public enum  OfferStatus {

    OPEN,
    PENDING,
    EXPIRED,
    CANCEL,
    WAITING_APPROVAL,
    CLOSED;

    public static OfferStatus getType(int ordinal){
        if(ordinal < 0 || ordinal > OfferStatus.values().length){
            return null;
        }
        return OfferStatus.values()[ordinal];
    }

    public boolean isOpen(){
        return this.equals(OfferStatus.OPEN);
    }
    public boolean isClosed(){
        return this.equals(OfferStatus.CLOSED);
    }
    public boolean isWaitingForApproval(){
        return this.equals(OfferStatus.WAITING_APPROVAL);
    }
}
