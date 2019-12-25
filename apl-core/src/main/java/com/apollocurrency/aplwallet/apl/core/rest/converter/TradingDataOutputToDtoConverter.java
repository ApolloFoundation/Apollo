/*
 * Copyright © 2018 - 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.TradingDataOutputDTO;
import com.apollocurrency.aplwallet.api.trading.TradingDataOutput;

/**
 *
 * @author Serhiy Lymar
 */
public class TradingDataOutputToDtoConverter implements Converter< TradingDataOutput, TradingDataOutputDTO > {

    @Override
    public TradingDataOutputDTO apply(TradingDataOutput tradingDataOutput) {                
        TradingDataOutputDTO tradingDataOutputDTO = new TradingDataOutputDTO();  
        tradingDataOutputDTO.Response = tradingDataOutput.getResponse();
        tradingDataOutputDTO.Type = tradingDataOutput.getType();
        tradingDataOutputDTO.Aggregated = tradingDataOutput.isAggregated();
        tradingDataOutputDTO.Data = tradingDataOutput.getData();
        tradingDataOutputDTO.TimeTo = tradingDataOutput.getTimeTo();
        tradingDataOutputDTO.TimeFrom = tradingDataOutput.getTimeFrom();
        tradingDataOutputDTO.ConversionType = tradingDataOutput.getConversionType();
        tradingDataOutputDTO.FirstValueInArray = tradingDataOutput.isFirstValueInArray();
        tradingDataOutputDTO.RateLimit = tradingDataOutput.getRateLimit();
        tradingDataOutputDTO.HasWarning = tradingDataOutput.isHasWarning();
        return tradingDataOutputDTO;
    }
}
