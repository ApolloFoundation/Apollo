/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexTradeEntryMin;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Serhiy Lymar
 */

@Singleton
public class TradingViewServiceImpl implements TradingViewService{
    
    private static final Logger log = LoggerFactory.getLogger(TransportInteractionServiceImpl.class);

    @Setter
    private volatile boolean done;
    
    @Override
    public List<DexTradeEntryMin> getTradeInfoForInterval(DexCurrencies pairCurrency, Integer start, Integer end) {
        List<DexTradeEntryMin> result = new ArrayList<>();
        return result;
    }

    
    void tick() {
        
    }
    
    @Override
    public void start() {
        
            log.debug("Trading view service startup point: ");
            done = false;
         
            Runnable task = () -> {
                for(;;) {                    
                    try {                        
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        log.error( "Runnable exception: {} ", ex.toString() );
                    }                    
                    this.tick();                   
                    if (done) break;
                }
                
            };
            Thread thread = new Thread(task);
            thread.start();

        
        
    }

    @Override
    public void stop() {
        this.done = true; 

    }
    
}
