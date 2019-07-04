/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.service;


import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOfferDBMatchingRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
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
    DexService dexService;
    private EpochTime epochTime;
        

    @Inject
    DexMatcherServiceImpl( DexService dexService, EpochTime epochTime ) {
        this.dexService =  Objects.requireNonNull( dexService,"dexService is null");
        this.epochTime =  Objects.requireNonNull( epochTime,"epochTime is null");        
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
    
        OfferType counterOfferType = createdOffer.getType().isSell() ? OfferType.BUY : OfferType.SELL;

        Integer currentTime = epochTime.getEpochTime();        
        BigDecimal offerAmount = new BigDecimal(createdOffer.getOfferAmount());        
        Integer pairCurrency = DexCurrencies.getValue( createdOffer.getPairCurrency());
        
        BigDecimal pairRate = new BigDecimal( EthUtil.ethToGwei( createdOffer.getPairRate()) ); 
        
        log.debug("Dumping arguments: type: {}, currentTime: {}, offerAmount: {}, offerCurrency: {}, pairRate: {}", counterOfferType, currentTime, offerAmount, pairCurrency, pairRate );
                
        DexOfferDBMatchingRequest  dexOfferDBMatchingRequest =  new DexOfferDBMatchingRequest(counterOfferType, currentTime,  0 , offerAmount, pairCurrency.intValue(), pairRate );        
        List<DexOffer> offers = dexService.getOffersForMatching(dexOfferDBMatchingRequest);
                       
        // DexOffer match = offers.
        int nOffers = offers.size();
        log.debug("offers found: {}", nOffers );
        
        if ( nOffers >= 1) {                        
            for (int i=0; i<nOffers; i++) {
                DexOffer currentOffer = offers.get(i);                
                if (validateOffer(currentOffer)) {
                    // matched...
                    log.debug("match found: {}", currentOffer.getId() );                    
                    return currentOffer;
                }
            }                     
        }

        return null;
    }
    
}
