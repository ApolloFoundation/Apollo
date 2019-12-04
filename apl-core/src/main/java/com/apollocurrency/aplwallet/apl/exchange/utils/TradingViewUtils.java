/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.utils;

import com.apollocurrency.aplwallet.api.trading.ConversionType;
import com.apollocurrency.aplwallet.api.trading.SimpleTradingEntry;
import com.apollocurrency.aplwallet.api.trading.TradingDataOutput;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.HeightDbIdRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexTradeEntry;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
    
    static public SimpleTradingEntry getDataForPeriodFromOffers(List<DexOrder> dexOrders, long start, long finish) {
        SimpleTradingEntry result = new SimpleTradingEntry();
        
        List<DexOrder> periodEntries = new ArrayList<>();
        dexOrders.forEach((entry)-> {                
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
            
            for(DexOrder entryOfPeriod: periodEntries) {            
                if ( entryOfPeriod.getPairRate().compareTo( hi ) == 1 ) {
                    hi = entryOfPeriod.getPairRate();
                }                
                if ( entryOfPeriod.getPairRate().compareTo( low ) == -1 ) {
                    low = entryOfPeriod.getPairRate();
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
        }
        return result;         
    }    
    
    
            
    static public TradingDataOutput getTestDataForInterval( String fsym, String tsym, Integer toTs, Integer limit, Integer interval) {
            
        log.debug("getTestDataForInterval:  fsym: {}, tsym: {}, toTs: {}, limit: {}", fsym, tsym, toTs, limit);

            int initialTime = toTs - (interval*2000);
            int startGraph = initialTime;
            
            TradingDataOutput tradingDataOutput = new TradingDataOutput();
            
            tradingDataOutput.setResponse("Success");
            tradingDataOutput.setType(100);
            tradingDataOutput.setAggregated(false);
            
            
            List<SimpleTradingEntry> data = new ArrayList<>();
            
            Random r = new Random();
            
            BigDecimal prevClose= BigDecimal.TEN;
            
            for (int i=0; i< limit; i++) {
                                                                    
                SimpleTradingEntry randomEntry = new SimpleTradingEntry();
                randomEntry.time = initialTime;
                
                boolean sign = (r.nextInt(2) == 1);
                
                double rWidth = r.nextInt(50) + r.nextDouble();
                
                if (sign) rWidth = -rWidth;
                                
                randomEntry.open =  prevClose;
                randomEntry.close =  randomEntry.open.add( BigDecimal.valueOf(rWidth));
                
                int rHigh = 50;// 15+ r.nextInt(25);
                int rLow = 50;// 15+r.nextInt(25);
                
                if (rWidth>0) {                    
                    randomEntry.high = randomEntry.close.add (BigDecimal.valueOf(rHigh));
                    randomEntry.low = randomEntry.open.subtract(BigDecimal.valueOf(rLow));
                } else {
                    randomEntry.high = randomEntry.open.add(BigDecimal.valueOf(rHigh));
                    randomEntry.low = randomEntry.close.subtract(BigDecimal.valueOf(rLow));
                }                
                prevClose = randomEntry.close;                                
                randomEntry.volumefrom = BigDecimal.valueOf( r.nextInt(10) );
                randomEntry.volumeto =  BigDecimal.valueOf( r.nextInt(50) );
                initialTime += interval;                
                data.add(randomEntry);
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
    
    
    static public TradingDataOutput getDataForInterval( String fsym, String tsym, Integer toTs, Integer limit, Integer interval, DexService service) {
            int initialTime = toTs - (interval*limit);
            int startGraph = initialTime;

            
            long startTS = (long)initialTime * 1000L;
            long endTS = (long)toTs * 1000L;    
            
            byte currencyType = 0;
            
            if (tsym.equals("ETH")) {                
                currencyType = 1;
                } else if (tsym.equals("PAX")) {                
                currencyType = 0;
                }
                            
            log.debug("start: {}, finish: {}, currencyType: {}", startTS, endTS, currencyType ); 
            
            List<DexTradeEntry> dexTradeEntries = service.getTradeInfoForPeriod(startTS, endTS, (byte)currencyType, 0, Integer.MAX_VALUE);
            
            TradingDataOutput tradingDataOutput = new TradingDataOutput();
            
            tradingDataOutput.setResponse("Success");
            tradingDataOutput.setType(100);
            tradingDataOutput.setAggregated(false);
            
            List<SimpleTradingEntry> data = new ArrayList<>();
            
            BigDecimal prevClose= BigDecimal.TEN;
            
            log.debug("extracted: {} values", dexTradeEntries.size() );
            
            for (int i=0; i< limit; i++) {                
                long start = (long)initialTime * 1000;
                long finish = 60000 + start ;
                SimpleTradingEntry entryForPeriod = TradingViewUtils.getDataForPeriod(dexTradeEntries, start, finish); 
                entryForPeriod.time = initialTime;
                entryForPeriod.open =  prevClose;
                prevClose = entryForPeriod.close;                                
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
    
    
        static public TradingDataOutput getDataForIntervalFromOffers( String fsym, String tsym, Integer toTs, Integer limit, Integer interval, DexService service) {
            int initialTime = toTs - (interval*limit);
            int startGraph = initialTime;

            
            long startTS = (long)initialTime * 1000L;
            long endTS = (long)toTs * 1000L;    
            
            byte currencyType = 0;
            
            if (tsym.equals("ETH")) {                
                currencyType = 1;
                } else if (tsym.equals("PAX")) {                
                currencyType = 0;
                }
                            
            log.debug("start: {}, finish: {}, currencyType: {}", startTS, endTS, currencyType ); 
            
            // List<DexTradeEntry> dexTradeEntries = service.getTradeInfoForPeriod(startTS, endTS, (byte)currencyType, 0, Integer.MAX_VALUE);
            HeightDbIdRequest heightDbIdRequest = new HeightDbIdRequest(startTS, endTS, currencyType, 0, Integer.MAX_VALUE);
            List<DexOrder> dexOrdersForInterval = service.getOrdersForTrading(heightDbIdRequest);
           
            
            TradingDataOutput tradingDataOutput = new TradingDataOutput();
            
            tradingDataOutput.setResponse("Success");
            tradingDataOutput.setType(100);
            tradingDataOutput.setAggregated(false);
            
            List<SimpleTradingEntry> data = new ArrayList<>();
            
            BigDecimal prevClose= BigDecimal.TEN;
            
            log.debug("extracted: {} values", dexOrdersForInterval.size() );
            
            for (int i=0; i< limit; i++) {                
                long start = (long)initialTime * 1000;
                long finish = 60000 + start ;
                SimpleTradingEntry entryForPeriod = TradingViewUtils.getDataForPeriodFromOffers(dexOrdersForInterval, start, finish); 
                entryForPeriod.time = initialTime;
                entryForPeriod.open =  prevClose;
                prevClose = entryForPeriod.close;                                
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
