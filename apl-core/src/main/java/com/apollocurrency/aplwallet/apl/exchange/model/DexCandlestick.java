package com.apollocurrency.aplwallet.apl.exchange.model;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class DexCandlestick {
    private DexCurrency coin; // paired coin name, base coin is always apl
    private BigDecimal min; // min price
    private BigDecimal max; // max price
    private BigDecimal open; // open price
    private BigDecimal close; // close price
    private BigDecimal fromVolume; // apl coin volume
    private BigDecimal toVolume; // paired coin volume
    private int timestamp; // seconds since unix epoch
}
