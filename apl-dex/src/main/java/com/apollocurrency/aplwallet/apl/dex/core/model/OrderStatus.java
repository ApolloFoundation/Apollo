package com.apollocurrency.aplwallet.apl.dex.core.model;

public enum OrderStatus {

    OPEN,       //0
    PENDING,    //1
    EXPIRED,    //2
    CANCEL,     //3
    WAITING_APPROVAL,   //4
    CLOSED,     //5
    PHASING_RESULT_PENDING; // 6

    public static OrderStatus getType(int ordinal) {
        if (ordinal < 0 || ordinal > OrderStatus.values().length) {
            return null;
        }
        return OrderStatus.values()[ordinal];
    }

    public boolean isOpen() {
        return this == OrderStatus.OPEN;
    }

    public boolean isPending() {
        return this == OrderStatus.PENDING;
    }

    public boolean isClosed() {
        return this == OrderStatus.CLOSED;
    }

    public boolean isWaitingForApproval() {
        return this == OrderStatus.WAITING_APPROVAL;
    }

    public boolean isClosedOrExpiredOrCancel() {
        return this == OrderStatus.CLOSED || this == OrderStatus.EXPIRED || this == OrderStatus.CANCEL;
    }
}
