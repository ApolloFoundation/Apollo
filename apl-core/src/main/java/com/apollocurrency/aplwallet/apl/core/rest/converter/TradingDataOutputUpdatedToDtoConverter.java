/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.TradingDataOutputUpdatedDTO;
import com.apollocurrency.aplwallet.api.trading.TradingDataOutputUpdated;

/**
 *
 * @author Serhiy Lymar
 */
public class TradingDataOutputUpdatedToDtoConverter implements Converter< TradingDataOutputUpdated, TradingDataOutputUpdatedDTO > {

    @Override
    public TradingDataOutputUpdatedDTO apply(TradingDataOutputUpdated t) {
        TradingDataOutputUpdatedDTO tradingDataOutputUpdatedDTO = new TradingDataOutputUpdatedDTO();  
        tradingDataOutputUpdatedDTO.c = t.getC();
        tradingDataOutputUpdatedDTO.h = t.getH();
        tradingDataOutputUpdatedDTO.l = t.getL();
        tradingDataOutputUpdatedDTO.o = t.getO();
        tradingDataOutputUpdatedDTO.s = t.getS();
        tradingDataOutputUpdatedDTO.t = t.getT();
        tradingDataOutputUpdatedDTO.v = t.getV();
        tradingDataOutputUpdatedDTO.nextTime = t.getNextTime();
        return tradingDataOutputUpdatedDTO;
    }
    
}
