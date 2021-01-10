package com.apollocurrency.aplwallet.apl.exchange.service.graph;

import com.apollocurrency.aplwallet.apl.dex.exchange.model.DexCandlestick;

@FunctionalInterface
public interface CandlestickLoader {
    DexCandlestick load(int openTime);
}
