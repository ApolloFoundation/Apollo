/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.utils;

import com.apollocurrency.aplwallet.api.trading.ConversionType;
import com.apollocurrency.aplwallet.api.trading.SimpleTradingEntry;
import com.apollocurrency.aplwallet.api.trading.TradingDataOutput;
import com.apollocurrency.aplwallet.api.trading.TradingDataOutputUpdated;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrderDBRequestForTrading;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.util.Constants;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Serhiy Lymar
 */
public class TradingViewUtils {
    
    private static final Logger log = LoggerFactory.getLogger(TradingViewUtils.class);

    
    public static SimpleTradingEntry getDataForPeriodFromOffersEpoch(List<DexOrder> dexOrders, Integer start, Integer finish) {
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
            if (log.isTraceEnabled()) {
                log.trace("offers: {}", periodEntries.size());
            }

            BigDecimal hi =  periodEntries.get(0).getPairRate();            
            BigDecimal low =  periodEntries.get(0).getPairRate(); 
            BigDecimal open = periodEntries.get(0).getPairRate();
            BigDecimal close = periodEntries.get( periodEntries.size()-1 ).getPairRate();



            BigDecimal volumefrom = BigDecimal.ZERO;
            BigDecimal volumeto = BigDecimal.ZERO; 
            
            for(DexOrder entryOfPeriod: periodEntries) {   
                
                BigDecimal currentPairRate = entryOfPeriod.getPairRate();
                if (log.isTraceEnabled()) {
                    log.trace("TS: {}, pairRate: {}", Convert2.fromEpochTime(entryOfPeriod.getFinishTime()), currentPairRate);
                }
                
                if ( currentPairRate.compareTo(hi) == 1 ) {
                    hi = currentPairRate;
                }

                if ( currentPairRate.compareTo(low) == -1 ) {
                    low = currentPairRate;
                } 

                BigDecimal amount = EthUtil.fromAtm( BigDecimal.valueOf(entryOfPeriod.getOrderAmount()) );
                BigDecimal vx = amount.multiply(entryOfPeriod.getPairRate());
                
                if (log.isTraceEnabled()) {
                    log.trace("amount: {}, rate: {}", amount, entryOfPeriod.getPairRate() );
                }
                
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





        
    public static TradingDataOutputUpdated getUpdatedDataForIntervalFromOffers( String symbol, String resolution, Integer toTs, Integer fromTs, DexService service, TimeService timeService) {

        int initialTime = fromTs;
        int finalTime = toTs;
        
        long startTS = (long)fromTs * 1000L;
        long endTS = (long)toTs * 1000L; 
            
                 
        int interval = 0;
        
        byte currencyType = 0;
        int intervalDiscretion=0, multiplier=1; 
                
        boolean onlyNumericInput = true;
        try {
            interval = Integer.parseInt(resolution);                       
        } catch (NumberFormatException e) {
            onlyNumericInput = false;
        }
        
        if(!onlyNumericInput) {   
            
            if (resolution.endsWith("D")) {
                intervalDiscretion = Constants.DEX_INTERVAL_DAY; 
                } else if (resolution.endsWith("H")) {
                intervalDiscretion = Constants.DEX_INTERVAL_HOUR; 
                } else if (resolution.endsWith("M")) {
                intervalDiscretion = Constants.DEX_INTERVAL_MIN;
            }                    
            
            if (resolution.length() > 1) {
                String mutlStr = resolution.substring(0, resolution.length() - 1);
                multiplier = Integer.valueOf(mutlStr,10);                        
            }                    
            interval = multiplier * intervalDiscretion;
        }
        
          
        
        
        int limit = (toTs - fromTs)/interval;
        
        if (log.isTraceEnabled()) {
            log.trace("discr: {}, mult: {}, interval: {}, limit: {} ", intervalDiscretion, multiplier, interval, limit);
        }
        
        if ( symbol.endsWith("ETH") ) {
            currencyType = 1;
        } else if ( symbol.endsWith("PAX") ) {
            currencyType = 2;
        } 
                                        
        if (log.isTraceEnabled()) {
            log.trace("start: {}, finish: {}, currencyType: {}, requestedType: {}",  new java.util.Date(startTS), new java.util.Date(endTS), currencyType );
        } 
                        
        Integer startTSEpoch = Convert2.toEpochTime(startTS);
        Integer endTSEpoch = Convert2.toEpochTime(endTS);
            
        if (log.isTraceEnabled() ) {
            log.trace("Epoch, start: {}, finish: {}", startTSEpoch, endTSEpoch );
        } 
            
        DexOrderDBRequestForTrading dexOrderDBRequestForTrading = new DexOrderDBRequestForTrading(startTSEpoch, endTSEpoch, currencyType, (byte)1, 0 , Integer.MAX_VALUE);
        
        List<DexOrder> dexOrdersForInterval = service.getOrdersForTrading(dexOrderDBRequestForTrading); 
        
        if (log.isTraceEnabled()) {
            log.trace("found {} orders", dexOrdersForInterval.size() );
        }

        for (DexOrder cr : dexOrdersForInterval) {
             if (log.isTraceEnabled()) {
                log.trace("order: {}, amount: {}, a1: {}, a2: {}, rate: {},", cr.getId(), cr.getOrderAmount(), EthUtil.weiToEther(BigInteger.valueOf(cr.getOrderAmount())),
                    EthUtil.fromAtm(BigDecimal.valueOf(cr.getOrderAmount())), cr.getPairRate());
            }
        }
            
        TradingDataOutput tradingDataOutput = new TradingDataOutput();            
        tradingDataOutput.setResponse("Success");
        tradingDataOutput.setType(100);
        tradingDataOutput.setAggregated(false);
            
        List<SimpleTradingEntry> data = new ArrayList<>();
            
        if (log.isTraceEnabled()) {
            log.trace("extracted: {} values", dexOrdersForInterval.size() );            
        }
            
        for (int i=0; i< limit; i++) {                
            
            long finish = finalTime * 1000L;
            long start = finish - (interval*1000L);
            
            if (log.isTraceEnabled()) {
                log.trace("start: {}, finish: {} ", start, finish );
            }
            
            Integer startEpoch =  Convert2.toEpochTime(start);
            Integer finishEpoch =  Convert2.toEpochTime(finish);
                
            SimpleTradingEntry entryForPeriod = TradingViewUtils.getDataForPeriodFromOffersEpoch(dexOrdersForInterval, startEpoch, finishEpoch ); 
            entryForPeriod.time = finalTime;
            
            if (dexOrdersForInterval.size() > 0 && !entryForPeriod.isZero()&& log.isTraceEnabled()) {
                log.trace ("interval data added, i: {} ts: {}, lo: {}, hi: {}, open: {}, close : {}", i, entryForPeriod.time, entryForPeriod.low, entryForPeriod.high, entryForPeriod.open, entryForPeriod.close);
                }
            finalTime -= interval;
            
            if (!entryForPeriod.isZero()) {
                data.add(entryForPeriod);
            }
            
        }
        
        Collections.reverse(data);
        
        TradingDataOutputUpdated tdo = new TradingDataOutputUpdated();
        
        tdo.setT(new ArrayList<>());
        tdo.setC(new ArrayList<>());
        tdo.setH(new ArrayList<>());
        tdo.setO(new ArrayList<>());
        tdo.setL(new ArrayList<>());
        tdo.setV(new ArrayList<>());

        
        for (SimpleTradingEntry e : data) {                                    
            tdo.getT().add(e.time);
            tdo.getC().add(e.close);
            tdo.getH().add(e.high);
            tdo.getO().add(e.open);
            tdo.getL().add(e.low);
            tdo.getV().add(e.volumefrom);
            
        }
        tdo.setS("ok");
        tdo.setNextTime(null);   
        return tdo;    
    }
}



// TradingDataOutputUpdated