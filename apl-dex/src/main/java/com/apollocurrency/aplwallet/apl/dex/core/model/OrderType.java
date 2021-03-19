/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.dex.core.model;

public enum OrderType {

    BUY,
    SELL;


    public static OrderType getType(int ordinal) {
        if (ordinal < 0 || ordinal > OrderType.values().length) {
            throw new IllegalArgumentException("Offer type with order: " + ordinal + " doesn't exist.");
        }
        return OrderType.values()[ordinal];
    }

    public boolean isBuy() {
        return this.ordinal() == OrderType.BUY.ordinal();
    }

    public boolean isSell() {
        return this.ordinal() == OrderType.SELL.ordinal();
    }

    public OrderType reverse() {
        return isBuy() ? OrderType.SELL : OrderType.BUY;
    }

}
