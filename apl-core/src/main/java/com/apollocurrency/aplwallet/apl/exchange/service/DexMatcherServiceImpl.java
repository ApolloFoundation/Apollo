/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.service;


import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOfferDBMatchingRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderType;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_ERROR_IN_PARAMETER;
import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_OK;
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
    private IDexValidator dexValidator;
        

    @Inject
    DexMatcherServiceImpl( DexMatchingService dexMatchingService, TimeService timeService, DexValidationServiceImpl dexValidationServiceImpl) {
        this.dexMatchingService =  Objects.requireNonNull( dexMatchingService,"dexService is null");
        this.timeService =  Objects.requireNonNull(timeService,"epochTime is null");
        this.dexValidator = Objects.requireNonNull( dexValidationServiceImpl,"dexService is null");
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
     * @param DexOffer  myOffer - created offer to validate
     * @param DexOffer  hisOffer - matched offer
    */ 
    private int validateOfferBuyAplEth(DexOrder myOrder, DexOrder hisOrder) {        
        return dexValidator.validateOfferBuyAplEth(myOrder, hisOrder);         
    }

    /**
     * currency-specific validation (Ethereum)
     * @param DexOffer  myOffer - created offer to validate
     * @param DexOffer  hisOffer - matched offer
    */     
    private int validateOfferSellAplEth(DexOrder myOffer, DexOrder hisOrder) {                
        return dexValidator.validateOfferSellAplEth(myOffer, hisOrder);      
    }

    /**
     * currency-specific validation (Pax)
     * @param DexOffer  myOffer - created offer to validate
     * @param DexOffer  hisOffer - matched offer
    */ 
    private int validateOfferBuyAplPax(DexOrder myOrder, DexOrder hisOrder) {
        return dexValidator.validateOfferBuyAplPax(myOrder, hisOrder);       
    }

    /**
     * currency-specific validation (Pax)
     * @param DexOffer  myOffer - created offer to validate
     * @param DexOffer  hisOffer - matched offer
    */ 
    private int validateOfferSellAplPax( DexOrder myOrder, DexOrder hisOrder) {
        return dexValidator.validateOfferSellAplPax(myOrder, hisOrder);         
    }    
        
    
    /**
     * Common validation routine for offer  
     * @param DexOffer  offer - offer to validate
     */ 
    private int validateOffer(DexOrder myOrder, DexOrder hisOrder) {
        
        DexCurrencies curr = hisOrder.getPairCurrency();
        log.debug("my order: orderCurrency: {}, pairCurrency: {}", myOrder.getOrderCurrency(), myOrder.getPairCurrency() );
        log.debug("his order: orderCurrency: {}, pairCurrency: {}", hisOrder.getOrderCurrency(), hisOrder.getPairCurrency() );
        
        switch (curr) {

            case ETH: { 
                // return validateOfferETH(myOffer,hisOffer);
                if (myOrder.getType() == OrderType.SELL) 
                    return validateOfferSellAplEth(myOrder, hisOrder); 
                else return validateOfferBuyAplEth(myOrder, hisOrder);                
            }
            
            case PAX: {
                if (myOrder.getType() == OrderType.SELL) 
                    return validateOfferSellAplPax(myOrder, hisOrder); 
                else return validateOfferBuyAplPax(myOrder, hisOrder);                                
            }
            
            default: return OFFER_VALIDATE_ERROR_IN_PARAMETER;
        }        
        
    }
    
            

    /**
     * Core event for matcher - when offer is created, it is called back     
     * @param DexOffer  myOffer - some data of offer, that is being created
     * @param DexOffer  hisOffer - the most suitable offer the Deal
     */

    private void onOfferMatch(DexOrder myOffer, DexOrder hisOffer) {
        log.debug("DexMatcherService.onOfferMatch callback ");
        log.debug("match..  id: {}, offerCurrency: {}, offerAmount: {}, pairCurrency: {}, pairRate: {} ", hisOffer.getAccountId(), hisOffer.getOrderCurrency(), hisOffer.getOrderAmount(), hisOffer.getPairCurrency(), hisOffer.getPairRate());
    }
    
        
    /**
     * Core event for matcher - when offer is created, it is called back     
     * @param offerType  Type of the offer. (BUY/SELL) 0/1
     */
    public void onCreateOffer(DexOrder createdOffer) {
        DexOrder pairOffer = findCounterOffer(createdOffer);
        if(pairOffer != null) {
            onOfferMatch(createdOffer, pairOffer);
        }
    }

    @Override
    public DexOrder findCounterOffer(DexOrder createdOffer) {
        log.debug("DexMatcherServiceImpl:findCounterOffer()");
    
        // it should be done the opposite way
        OrderType counterOrderType = createdOffer.getType().isBuy() ? OrderType.SELL : OrderType.BUY;
        // Be careful: for selling - it should be more expensive. Buying - for cheaper price.  
        String orderby = createdOffer.getType().isSell() ? "DESC" : "ASC";
        
        Integer currentTime = timeService.getEpochTime();
        BigDecimal offerAmount = new BigDecimal(createdOffer.getOrderAmount());
        Integer pairCurrency = DexCurrencies.getValue( createdOffer.getPairCurrency());
        
        BigDecimal pairRate = new BigDecimal( EthUtil.ethToGwei( createdOffer.getPairRate()) );

        log.debug("Dumping arguments: type: {}, currentTime: {}, offerAmount: {}, offerCurrency: {}, pairRate: {}, order: {}",
                counterOrderType, currentTime, offerAmount, pairCurrency, pairRate, orderby);

        DexOfferDBMatchingRequest dexOfferDBMatchingRequest = new DexOfferDBMatchingRequest(counterOrderType, currentTime, 0, offerAmount, pairCurrency.intValue(), pairRate, orderby);
        List<DexOrder> offers = dexMatchingService.getOffersForMatching(dexOfferDBMatchingRequest, orderby);
        
        int nOffers = offers.size();
        log.debug("offers found: {}", nOffers );

        if ( nOffers >= 1) {
            DexOrder counterOffer = offers.get(0);
            if (validateOffer(createdOffer, counterOffer) == OFFER_VALIDATE_OK) {                    
                    log.debug("match found, id: {}, amount: {}, pairCurrency: {}, pairRate: {}  ", counterOffer.getId(),
                            counterOffer.getOrderAmount(), counterOffer.getPairCurrency(),
                            counterOffer.getPairRate() );                    
                    return counterOffer;
                }
        }
        return null;
    }
    
}
