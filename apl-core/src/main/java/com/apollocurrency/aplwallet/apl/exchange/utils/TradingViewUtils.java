/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.utils;

import com.apollocurrency.aplwallet.api.trading.ConversionType;
import com.apollocurrency.aplwallet.api.trading.SimpleTradingEntry;
import com.apollocurrency.aplwallet.api.trading.TradingDataOutput;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrderDBRequestForTrading;
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

    
    static public SimpleTradingEntry getDataForPeriodFromOffersEpoch(List<DexOrder> dexOrders, Integer start, Integer finish) {
        // request type =1 or 0 depending on sale or buy
        
        SimpleTradingEntry result = new SimpleTradingEntry();
        
        List<DexOrder> periodEntries = new ArrayList<>();
        dexOrders.forEach((entry)-> {                
            long finishTS = entry.getFinishTime();                
            if (finishTS >= start && finishTS < finish) {                                   
                periodEntries.add(entry);                
            }                            
        });
        
        if (periodEntries.size() > 0) { 
            log.debug("offers: {}", periodEntries.size());

//            BigDecimal hi = new BigDecimal( EthUtil.etherToWei( periodEntries.get(0).getPairRate() ) );            
//            BigDecimal low = new BigDecimal( EthUtil.etherToWei( periodEntries.get(0).getPairRate() )); 
//            BigDecimal open = new BigDecimal( EthUtil.etherToWei( periodEntries.get(0).getPairRate() ));
//            BigDecimal close = new BigDecimal( EthUtil.etherToWei( periodEntries.get( periodEntries.size()-1 ).getPairRate() ));
            BigDecimal hi =  periodEntries.get(0).getPairRate();            
            BigDecimal low =  periodEntries.get(0).getPairRate(); 
            BigDecimal open = periodEntries.get(0).getPairRate();
            BigDecimal close = periodEntries.get( periodEntries.size()-1 ).getPairRate();



            BigDecimal volumefrom = BigDecimal.ZERO;
            BigDecimal volumeto = BigDecimal.ZERO; 
            
            for(DexOrder entryOfPeriod: periodEntries) {   
                
                
                BigDecimal currentPairRate = entryOfPeriod.getPairRate();
                log.debug("TS: {}, pairRate: {}", Convert2.fromEpochTime(entryOfPeriod.getFinishTime()), currentPairRate);
                
                if ( currentPairRate.compareTo(hi) == 1 ) {
                    hi = currentPairRate;
                }

                if ( currentPairRate.compareTo(low) == -1 ) {
                    low = currentPairRate;
                } 
                BigDecimal amount = BigDecimal.valueOf( entryOfPeriod.getOrderAmount() );
                BigDecimal vx = BigDecimal.valueOf(entryOfPeriod.getOrderAmount()).multiply(entryOfPeriod.getPairRate());
                volumefrom = volumefrom.add(amount);
                volumeto = volumeto.add(vx);
            }            
            result.low = low; 
            result.high = hi;
            result.open = open; 
            result.close = close;             
            result.volumefrom = volumefrom;
            result.volumeto = volumeto;            
        } else {
            result.low = BigDecimal.ZERO;
            result.high = BigDecimal.ZERO;
            result.open = BigDecimal.ZERO;
            result.close = BigDecimal.ZERO;             
            result.volumefrom = BigDecimal.ZERO;
            result.volumeto = BigDecimal.ZERO;     
        }
        return result;         
    }        


        
    static public TradingDataOutput getDataForIntervalFromOffers( String fsym, String tsym, Integer toTs, Integer limit, Integer interval, DexService service, TimeService timeService) {
        int initialTime = toTs - (interval*limit);
        int startGraph = initialTime;

            
        long startTS = (long)initialTime * 1000L;
        long endTS = (long)toTs * 1000L; 
            
        Integer toTS = Convert2.toEpochTime(startTS);
                                   
        byte currencyType = 0;
        
        if ( tsym.equals("ETH") || fsym.equals("ETH") ) {
            currencyType = 1;
        } 
                
        if ( tsym.equals("PAX") || fsym.equals("PAX") ) {
            currencyType = 0;
        } 
                                        
        log.debug("start: {}, finish: {}, currencyType: {}, requestedType: {}", startTS, endTS, currencyType ); 
                        
        Integer startTSEpoch = Convert2.toEpochTime(startTS);
        Integer endTSEpoch = Convert2.toEpochTime(endTS);
            
        log.debug("Epoch, start: {}, finish: {}", startTSEpoch, endTSEpoch ); 
            
        DexOrderDBRequestForTrading dexOrderDBRequestForTrading = new DexOrderDBRequestForTrading(startTSEpoch, endTSEpoch, (byte)1, currencyType, 0 , Integer.MAX_VALUE);
            
        List<DexOrder> dexOrdersForInterval = service.getOrdersForTrading(dexOrderDBRequestForTrading); 
        
        log.debug("found {} orders", dexOrdersForInterval.size() );
        for (DexOrder cr : dexOrdersForInterval) {
            log.debug("order: {}, amount: {}, rate: {},", cr.getId(), cr.getOrderAmount(), cr.getPairRate());
        }
            
        TradingDataOutput tradingDataOutput = new TradingDataOutput();            
        tradingDataOutput.setResponse("Success");
        tradingDataOutput.setType(100);
        tradingDataOutput.setAggregated(false);
            
        List<SimpleTradingEntry> data = new ArrayList<>();
            
        BigDecimal prevClose= BigDecimal.ZERO;
            
        log.debug("extracted: {} values", dexOrdersForInterval.size() );
            
        for (int i=0; i< limit; i++) {                
            long start = (long)initialTime * 1000;
            long finish = (interval * 1000L) + start ;                
            Integer startEpoch =  Convert2.toEpochTime(start);
            Integer finishEpoch =  Convert2.toEpochTime(finish);
                
            SimpleTradingEntry entryForPeriod = TradingViewUtils.getDataForPeriodFromOffersEpoch(dexOrdersForInterval, startEpoch, finishEpoch ); 
            entryForPeriod.time = initialTime;
            
            if (dexOrdersForInterval.size() > 0) {
                log.debug ("interval data added, i: {} ts: {}, lo: {}, hi: {}, open: {}, close : {}", i, entryForPeriod.time, entryForPeriod.low, entryForPeriod.high, entryForPeriod.open, entryForPeriod.close);
            }
            // entryForPeriod.open =  prevClose;
            // prevClose = entryForPeriod.close;                                
            initialTime += interval;                
            data.add(entryForPeriod);
            }
                
        tradingDataOutput.setData(data);
        tradingDataOutput.setTimeTo(toTs);
        tradingDataOutput.setTimeFrom(startGraph);
        tradingDataOutput.setFirstValueInArray(true);
        ConversionType conversionType = new ConversionType();
        conversionType.type = "force_direct";
        conversionType.conversionSymbol = "";
        tradingDataOutput.setConversionType(conversionType);
        tradingDataOutput.setHasWarning(false);
            
        return tradingDataOutput;
        }

}