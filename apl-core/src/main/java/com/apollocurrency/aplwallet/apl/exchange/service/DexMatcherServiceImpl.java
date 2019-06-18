/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.apl.exchange.service;


import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Serhiy Lymar
 */




@Singleton
public class DexMatcherServiceImpl implements IDexMatcherInterface {
    
    private static final Logger log = LoggerFactory.getLogger(DexMatcherServiceImpl.class);

    @Inject
    DexMatcherServiceImpl() {
        
    }
    
    /**
     * Start matcher service
     */
    
    @Override
    public void initialize() {
        log.debug("DexMatcherService : initialization routine");
    }

    /**
     * Stop matcher service
     */
    
    @Override
    public void deinitialize() {
        log.debug("DexMatcherService : deinitialization routine");        
    }
    

    /**
     * Core event for matcher - when offer is created, it is called back     
     * @param offerType  Type of the offer. (BUY/SELL) 0/1
     * @param walletAddress From address
     * @param offerAmount Offer amount in Gwei (1 Gwei = 0.000000001)
     * @param pairCurrency Paired currency. (APL=0, ETH=1, PAX=2)
     * @param pairRate Pair rate in Gwei. (1 Gwei = 0.000000001)
     * @param amountOfTime Amount of time for this offer. (seconds)
     */
    
    public void onCreateOffer ( Byte offerType,
                                String walletAddress,
                                Long offerAmount,   
                                Byte pairCurrency,
                                Long pairRate,
                                Integer amountOfTime ) {
        
    }
    
}
