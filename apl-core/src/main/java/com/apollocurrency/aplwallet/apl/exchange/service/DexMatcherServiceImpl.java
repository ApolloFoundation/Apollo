/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.service;


import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrderDBMatchingRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.apollocurrency.aplwallet.apl.util.Constants.OFFER_VALIDATE_OK;

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
    DexMatcherServiceImpl( DexMatchingService dexMatchingService, TimeService timeService, IDexValidator dexValidator) {
        this.dexMatchingService =  Objects.requireNonNull( dexMatchingService,"dexService is null");
        this.timeService =  Objects.requireNonNull(timeService,"epochTime is null");
        this.dexValidator = Objects.requireNonNull( dexValidator,"dexValidator is null");
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
  * @param DexOrder  myOffer - created offer to validate
  * @param DexOrder  hisOffer - matched offer
    */ 
    private int validateOfferBuyAplEth(DexOrder myOrder, DexOrder hisOrder) {        
        return dexValidator.validateOfferBuyAplEth(myOrder, hisOrder);         
    }

    /**
     * currency-specific validation (Ethereum)
     * @param DexOrder  myOffer - created offer to validate
     * @param DexOrder  hisOffer - matched offer
    */     
    private int validateOfferSellAplEth(DexOrder myOffer, DexOrder hisOrder) {                
        return dexValidator.validateOfferSellAplEth(myOffer, hisOrder);      
    }

    /**
     * currency-specific validation (Pax)
     * @param DexOrder  myOffer - created offer to validate
     * @param DexOrder  hisOffer - matched offer
    */ 
    private int validateOfferBuyAplPax(DexOrder myOrder, DexOrder hisOrder) {
        return dexValidator.validateOfferBuyAplPax(myOrder, hisOrder);       
    }

    /**
     * currency-specific validation (Pax)
     * @param DexOrder  myOffer - created offer to validate
     * @param DexOrder  hisOffer - matched offer
    */ 
    private int validateOfferSellAplPax( DexOrder myOrder, DexOrder hisOrder) {
        return dexValidator.validateOfferSellAplPax(myOrder, hisOrder);         
    }    
        
    
    /**
     * Common validation routine for offer  
     * @param DexOrder  offer - offer to validate
     */ 
    private int validateOffer(DexOrder myOrder, DexOrder hisOrder) throws Exception {
        
        DexCurrencies curr = hisOrder.getPairCurrency();
        log.debug("my order: orderCurrency: {}, pairCurrency: {}", myOrder.getOrderCurrency(), myOrder.getPairCurrency() );
        log.debug("his order: orderCurrency: {}, pairCurrency: {}", hisOrder.getOrderCurrency(), hisOrder.getPairCurrency() );
        
        switch (curr) {

            case ETH: { 
                // return validateOfferETH(myOffer,hisOffer);
                if (myOrder.getType() == OrderType.SELL) {
                    return validateOfferSellAplEth(myOrder, hisOrder); 
                } else {
                    return validateOfferBuyAplEth(myOrder, hisOrder);
                }                
            }
            
            case PAX: {
                if (myOrder.getType() == OrderType.SELL) {
                    return validateOfferSellAplPax(myOrder, hisOrder); 
                } else {
                    return validateOfferBuyAplPax(myOrder, hisOrder);
                }                                
            }
            
            default: throw new Exception("Provided currency is not supported yet");
        }        
        
    }
    
            

    /**
     * Core event for matcher - when offer is created, it is called back     
     * @param DexOrder  myOffer - some data of offer, that is being created
     * @param DexOrder  hisOffer - the most suitable offer the Deal
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
    public DexOrder findCounterOffer(DexOrder createdOrder) {
        log.debug("DexMatcherServiceImpl:findCounterOffer()");
    
        // it should be done the opposite way
        OrderType counterOrderType = createdOrder.getType().isBuy() ? OrderType.SELL : OrderType.BUY;
        // Be careful: for selling - it should be more expensive. Buying - for cheaper price.  
        String orderBy = createdOrder.getType().isSell() ? "DESC" : "ASC";
        
        Integer currentTime = timeService.getEpochTime();
        BigDecimal offerAmount = new BigDecimal(createdOrder.getOrderAmount());
        Integer pairCurrency = DexCurrencies.getValue(createdOrder.getPairCurrency());

        BigDecimal pairRate = new BigDecimal(EthUtil.ethToGwei(createdOrder.getPairRate()));

        log.debug("Dumping arguments: type: {}, currentTime: {}, offerAmount: {}, offerCurrency: {}, pairRate: {}, order: {}",
                counterOrderType, currentTime, offerAmount, pairCurrency, pairRate, orderBy);

        DexOrderDBMatchingRequest dexOrderDBMatchingRequest = new DexOrderDBMatchingRequest(counterOrderType, currentTime, 0, offerAmount, pairCurrency.intValue(), pairRate, orderBy);
        List<DexOrder> orders = dexMatchingService.getOffersForMatching(dexOrderDBMatchingRequest, orderBy);

        log.debug("offers found: {}", orders.size());

        //Skip your orders.
        List<DexOrder> filteredOrders = orders.stream()
                .filter(order -> !order.getAccountId().equals(createdOrder.getAccountId()))
                .collect(Collectors.toList());

        for (DexOrder counterOffer : filteredOrders) {
            try {
                if (validateOffer(createdOrder, counterOffer) == OFFER_VALIDATE_OK) {
                    log.debug("match found, id: {}, amount: {}, pairCurrency: {}, pairRate: {}  ", counterOffer.getId(),
                            counterOffer.getOrderAmount(), counterOffer.getPairCurrency(),
                            counterOffer.getPairRate());
                    return counterOffer;
                }
            } catch (Exception ex) {
                log.debug("Validation error: {}", ex.toString());
            }
        }

        return null;
    }
    
}
