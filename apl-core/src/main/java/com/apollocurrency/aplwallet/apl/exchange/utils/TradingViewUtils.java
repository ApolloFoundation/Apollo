/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.utils;

import com.apollocurrency.aplwallet.api.trading.SimpleTradingEntry;
import com.apollocurrency.aplwallet.apl.exchange.model.DexTradeEntry;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Serhiy Lymar
 */
public class TradingViewUtils {
    
    
    private static final Logger log = LoggerFactory.getLogger(TradingViewUtils.class);
    
    static public SimpleTradingEntry getDataForPeriod(List<DexTradeEntry> dexTradeEntries, long start, long finish) {
        SimpleTradingEntry result = new SimpleTradingEntry();
        
        List<DexTradeEntry> periodEntries = new ArrayList<>();
        dexTradeEntries.forEach((entry)-> {                
            long finishTS = entry.getFinishTime();                
            if (finishTS >= start && finishTS < finish) {                                   
                periodEntries.add(entry);                
            }                            
        });
        
        if (periodEntries.size() > 0) {            
            BigDecimal hi = periodEntries.get(0).getPairRate();            
            BigDecimal low = periodEntries.get(0).getPairRate();        
            BigDecimal open = periodEntries.get(0).getPairRate();
            BigDecimal close = periodEntries.get( periodEntries.size()-1 ).getPairRate();
            BigDecimal volumefrom = BigDecimal.ZERO;
            BigDecimal volumeto = BigDecimal.ZERO; 
            
            for(DexTradeEntry entryOfPeriod: periodEntries) {            
                if ( entryOfPeriod.getPairRate().compareTo( hi ) == 1 ) {
                    hi = entryOfPeriod.getPairRate();
                }                
                if ( entryOfPeriod.getPairRate().compareTo( low ) == -1 ) {
                    low = entryOfPeriod.getPairRate();
                }                
                BigDecimal amount = BigDecimal.valueOf( entryOfPeriod.getSenderOfferAmount() );
                BigDecimal vx = BigDecimal.valueOf(entryOfPeriod.getSenderOfferAmount()).multiply(entryOfPeriod.getPairRate());
                volumefrom = volumefrom.add(amount);
                volumeto = volumeto.add(vx);
            }            
            result.low = low; 
            result.high = hi;
            result.open = open; 
            result.close = close;             
            result.volumefrom = volumefrom;
            result.volumeto = volumeto;            
        }
        return result;         
    }
    
}
