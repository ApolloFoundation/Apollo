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
        return this == OfferStatus.OPEN;
    }

    public boolean isPending() {
        return this == OfferStatus.PENDING;
    }
    public boolean isClosed(){
        return this == OfferStatus.CLOSED;
    }
    public boolean isWaitingForApproval(){
        return this == OfferStatus.WAITING_APPROVAL;
    }
}
