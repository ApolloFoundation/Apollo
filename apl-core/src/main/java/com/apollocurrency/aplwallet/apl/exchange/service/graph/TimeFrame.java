package com.apollocurrency.aplwallet.apl.exchange.service.graph;

public enum TimeFrame {
    QUARTER(1, "15"), HOUR(4, "60"), FOUR_HOURS(16, "240"), DAY(96, "D");
    public final int muliplier; // BASE TIME INTERVAL multiplier
    public final String tvRepresentation;

    TimeFrame(int multiplier, String tvRepresentation) {
        this.muliplier = multiplier;
        this.tvRepresentation = tvRepresentation;
    }

    public static TimeFrame fromString(String value) {
        for (TimeFrame timeFrame : values()) {
            if (timeFrame.tvRepresentation.equalsIgnoreCase(value) || timeFrame.toString().equalsIgnoreCase(value)) {
                return timeFrame;
            }
        }
        throw new IllegalArgumentException("Unsupported or incorrect timeframe " + value);
    }
}
