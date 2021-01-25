/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.converter;

import javax.inject.Singleton;

import com.apollocurrency.aplwallet.api.dto.TradingDataOutputDTO;
import com.apollocurrency.aplwallet.api.trading.TradingDataOutput;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;

/**
 * @author Serhiy Lymar
 */
@Singleton
public class TradingDataOutputToDtoConverter implements Converter<TradingDataOutput, TradingDataOutputDTO> {

    @Override
    public TradingDataOutputDTO apply(TradingDataOutput t) {
        TradingDataOutputDTO tradingDataOutputDTO = new TradingDataOutputDTO();
        tradingDataOutputDTO.c = t.getC();
        tradingDataOutputDTO.h = t.getH();
        tradingDataOutputDTO.l = t.getL();
        tradingDataOutputDTO.o = t.getO();
        tradingDataOutputDTO.s = t.getS();
        tradingDataOutputDTO.t = t.getT();
        tradingDataOutputDTO.v = t.getV();
        tradingDataOutputDTO.nextTime = t.getNextTime();
        return tradingDataOutputDTO;
    }

}
