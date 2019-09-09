package com.apollocurrency.aplwallet.apl.exchange.model;

public enum OrderStatus {

    OPEN,
    PENDING,
    EXPIRED,
    CANCEL,
    WAITING_APPROVAL,
    CLOSED;

    public static OrderStatus getType(int ordinal) {
        if (ordinal < 0 || ordinal > OrderStatus.values().length) {
            return null;
        }
        return OrderStatus.values()[ordinal];
    }

    public boolean isOpen(){
        return this == OrderStatus.OPEN;
    }

    public boolean isPending() {
        return this == OrderStatus.PENDING;
    }
    public boolean isClosed(){
        return this == OrderStatus.CLOSED;
    }
    public boolean isWaitingForApproval(){
        return this == OrderStatus.WAITING_APPROVAL;
    }
}
