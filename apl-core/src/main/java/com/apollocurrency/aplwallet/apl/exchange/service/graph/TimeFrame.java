package com.apollocurrency.aplwallet.apl.exchange.service.graph;

public enum TimeFrame {
    QUARTER(1), HOUR(4), FOUR_HOURS(16), DAY(96);
    public final int muliplier; // BASE TIME INTERVAL multiplier
    TimeFrame(int multiplier) {
        this.muliplier = multiplier;
    }
}
