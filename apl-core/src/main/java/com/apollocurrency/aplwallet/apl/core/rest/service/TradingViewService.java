/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexTradeEntryMin;
import java.util.List;
import javax.inject.Singleton;

/**
 *
 * @author nemez
 */

@Singleton
public interface TradingViewService {
    
    
    List<DexTradeEntryMin> getTradeInfoForInterval(DexCurrencies pairCurrency, 
            Integer start, Integer end);    
    /** 
     * Start transport interaction service
     */
     
    public void start();
   
    /** 
     * Start transport interaction service
     */
     
    public void stop();
    
}
