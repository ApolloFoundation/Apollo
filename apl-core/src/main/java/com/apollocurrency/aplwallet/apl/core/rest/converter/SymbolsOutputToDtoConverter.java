/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.SymbolsOutputDTO;
import com.apollocurrency.aplwallet.api.trading.SymbolsOutput;

/**
 *
 * @author Serhiy Lymar
 */
public class SymbolsOutputToDtoConverter implements Converter< SymbolsOutput, SymbolsOutputDTO > {
    
    @Override
    public SymbolsOutputDTO apply(SymbolsOutput so) {        
        SymbolsOutputDTO symbolsOutputDTO = new SymbolsOutputDTO();
        symbolsOutputDTO.name = so.getName();   
        symbolsOutputDTO.exchange_traded = so.getExchange_traded();    
        symbolsOutputDTO.exchange_listed = so.getExchange_listed();
        symbolsOutputDTO.timezone = so.getTimezone();    
        symbolsOutputDTO.minmov = so.getMinmov();
        symbolsOutputDTO.minmov2 = so.getMinmov2();
        symbolsOutputDTO.pointvalue = so.getPointvalue();    
        symbolsOutputDTO.session = so.getSession();        
        // symbolsOutputDTO.has_intraday = so.getIntraday();
        // symbolsOutputDTO.has_no_volume = so.getNo_volume();        
        symbolsOutputDTO.description = so.getDescription();        
        symbolsOutputDTO.type = so.getType();    
        symbolsOutputDTO.supported_resolutions = so.getSupported_resolutions();
        symbolsOutputDTO.pricescale = so.getPricescale();
        symbolsOutputDTO.ticker = so.getTicker();          
        return symbolsOutputDTO;
        
        
    }                
    
}
