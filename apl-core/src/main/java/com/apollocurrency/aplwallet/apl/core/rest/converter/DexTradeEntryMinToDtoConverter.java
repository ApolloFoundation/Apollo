/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.DexTradeInfoMinDto;
import com.apollocurrency.aplwallet.apl.exchange.model.DexTradeEntryMin;

/**
 *
 * @author Serhiy Lymar
 */
public class DexTradeEntryMinToDtoConverter implements Converter< DexTradeEntryMin, DexTradeInfoMinDto > {
     @Override
    public DexTradeInfoMinDto apply(DexTradeEntryMin t) {
        DexTradeInfoMinDto dexTradeInfoMinDto = new DexTradeInfoMinDto();
        dexTradeInfoMinDto.low = t.getLow();
        dexTradeInfoMinDto.hi = t.getHi();
        dexTradeInfoMinDto.open = t.getOpen();
        dexTradeInfoMinDto.close = t.getClose();
        dexTradeInfoMinDto.volumefrom = t.getVolumefrom();
        dexTradeInfoMinDto.volumeto = t.getVolumeto();
        return dexTradeInfoMinDto;
    }
    
    
}
