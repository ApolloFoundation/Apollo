/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.service;


import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOfferDBMatchingRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author Serhiy Lymar
 */




@Singleton
public class DexMatcherServiceImpl implements IDexMatcherInterface {
    
    private static final Logger log = LoggerFactory.getLogger(DexMatcherServiceImpl.class);
    private DexMatchingService dexMatchingService;
    private TimeService timeService;
        

    @Inject
    DexMatcherServiceImpl( DexMatchingService dexMatchingService, TimeService timeService) {
        this.dexMatchingService =  Objects.requireNonNull( dexMatchingService,"dexService is null");
        this.timeService =  Objects.requireNonNull(timeService,"epochTime is null");
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
     * currency-specific validation (Ethereum)
     * @param DexOffer  offer to validate
     */ 
    private boolean validateOfferETH(DexOffer offer) {
        // TODO: add validation routine
        return true;         
    }

    /**
     * currency-specific validation (Apollo)
     * @param DexOffer  offer to validate
     */ 
    private boolean validateOfferAPL(DexOffer offer) {
        // TODO: add validation routine
        return true;         
    }
    
    /**
     * currency-specific validation (Pax)
     * @param DexOffer  offer to validate
     */ 
    private boolean validateOfferPAX(DexOffer offer) {
        // TODO: add validation routine
        return true;         
    }
    
    
    /**
     * Common validation routine for offer  
     * @param DexOffer  offer - offer to validate
     */ 
    private boolean validateOffer( DexOffer offer) {
        
        DexCurrencies curr = offer.getOfferCurrency(); 
        log.debug("currency: {}", curr );
        
        switch (curr) {
            case APL: return validateOfferAPL(offer);
            case ETH: return validateOfferETH(offer);
            case PAX: return validateOfferPAX(offer);
            default: return false;
        }        
        
    }
    
            

    /**
     * Core event for matcher - when offer is created, it is called back     
     * @param DexOffer  myOffer - some data of offer, that is being created
     * @param DexOffer  hisOffer - the most suitable offer the Deal
     */ 
    
    private void onOfferMatch ( DexOffer myOffer, DexOffer hisOffer) {
        log.debug("DexMatcherService.onOfferMatch callback ");
        log.debug("match..  id: {}, offerCurrency: {}, offerAmount: {}, pairCurrency: {}, pairRate: {} ", hisOffer.getAccountId(), hisOffer.getOfferCurrency(), hisOffer.getOfferAmount(), hisOffer.getPairCurrency(), hisOffer.getPairRate() ); 
    }
    
        
    /**
     * Core event for matcher - when offer is created, it is called back     
     * @param offerType  Type of the offer. (BUY/SELL) 0/1
     */
    public void onCreateOffer(DexOffer createdOffer){
        DexOffer pairOffer = findCounterOffer(createdOffer);
        if(pairOffer != null) {
            onOfferMatch(createdOffer, pairOffer);
        }
    }


    public DexOffer findCounterOffer(DexOffer createdOffer) {
        log.debug("DexMatcherServiceImpl:findCounterOffer()");
    
        // it should be done the opposite way
        OfferType counterOfferType = createdOffer.getType().isBuy() ? OfferType.SELL : OfferType.BUY;
        // Be careful: for selling - it should be more expensive. Buying - for cheaper price.  
        String orderby = createdOffer.getType().isSell() ? "DESC" : "ASC";
        
        Integer currentTime = timeService.getEpochTime();
        BigDecimal offerAmount = new BigDecimal(createdOffer.getOfferAmount());        
        Integer pairCurrency = DexCurrencies.getValue( createdOffer.getPairCurrency());
        
        BigDecimal pairRate = new BigDecimal( EthUtil.ethToGwei( createdOffer.getPairRate()) ); 
        
        log.debug("Dumping arguments: type: {}, currentTime: {}, offerAmount: {}, offerCurrency: {}, pairRate: {}, order: {}", 
                counterOfferType, currentTime, offerAmount, pairCurrency, pairRate, orderby );
                
        DexOfferDBMatchingRequest  dexOfferDBMatchingRequest =  new DexOfferDBMatchingRequest(counterOfferType, currentTime,  0 , offerAmount, pairCurrency.intValue(), pairRate, orderby );        
        List<DexOffer> offers = dexMatchingService.getOffersForMatching(dexOfferDBMatchingRequest, orderby);
        
        int nOffers = offers.size();
        log.debug("offers found: {}", nOffers );

        if ( nOffers >= 1) { 
            DexOffer counterOffer = offers.get(0);
            if (validateOffer(counterOffer)) {                    
                    log.debug("match found, id: {}, amount: {}, pairCurrency: {}, pairRate: {}  ", counterOffer.getId(), 
                            counterOffer.getOfferAmount(), counterOffer.getPairCurrency(), 
                            counterOffer.getPairRate() );                    
                    return counterOffer;
                }
        }
        return null;
    }
    
}
