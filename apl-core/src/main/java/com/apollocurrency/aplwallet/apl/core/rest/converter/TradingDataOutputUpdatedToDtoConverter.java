/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.TradingDataOutputDTO;
import com.apollocurrency.aplwallet.api.dto.TradingDataOutputUpdatedDTO;
import com.apollocurrency.aplwallet.api.trading.TradingDataOutput;
import com.apollocurrency.aplwallet.api.trading.TradingDataOutputUpdated;

/**
 *
 * @author nemez
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
