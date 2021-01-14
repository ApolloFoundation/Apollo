package com.apollocurrency.aplwallet.apl.dex.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DexCandlestick {
    private DexCurrency coin; // paired coin name, base coin is always apl
    private BigDecimal min; // min price
    private BigDecimal max; // max price
    private BigDecimal open; // open price
    private BigDecimal close; // close price
    private BigDecimal fromVolume; // apl coin volume
    private BigDecimal toVolume; // paired coin volume
    private int timestamp; // seconds since unix epoch
    private int openOrderTimestamp; // finish time of the earliest order included into this candlestick
    private int closeOrderTimestamp; // finish time of the latest order included into this candlestick

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DexCandlestick that = (DexCandlestick) o;
        return timestamp == that.timestamp &&
            openOrderTimestamp == that.openOrderTimestamp &&
            closeOrderTimestamp == that.closeOrderTimestamp &&
            coin == that.coin &&
            Objects.equals(min, that.min) &&
            Objects.equals(max, that.max) &&
            Objects.equals(open, that.open) &&
            Objects.equals(close, that.close) &&
            Objects.equals(fromVolume, that.fromVolume) &&
            Objects.equals(toVolume, that.toVolume);
    }

    @Override
    public int hashCode() {
        return Objects.hash(coin, min, max, open, close, fromVolume, toVolume, timestamp, openOrderTimestamp, closeOrderTimestamp);
    }
}
