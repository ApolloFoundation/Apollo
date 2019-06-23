/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.apl.exchange.service;


import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOfferDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferType;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
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
     * Core event for matcher - when offer is created, it is called back     
     * @param DexOffer  myOffer - some data of offer, that is being created
     * @param DexOffer  hisOffer - the most suitable offer the Deal
     */ 
    
    private void onOfferMatch ( DexOffer myOffer, DexOffer hisOffer) {
        
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
        
        log.debug("DexMatcherServiceImpl:onCreateOffer, offerType: {}, walletAddress: {}, offerAmount: {}, pairCurrency: {}, pairRate: {}, amountOfTime: {}", offerType, walletAddress, offerAmount, pairCurrency, pairRate, amountOfTime );
        
        
        OfferType type = null;
        OfferStatus offerStatus = null;
        DexCurrencies pairCur = null;
        Integer currentTime = null;
        Long accountId = null;
        
        boolean isAvailableForNow = true;
        String accountIdStr = "6141493552915739570";

        //Validate
        try {
            if (offerType != null) {
                type = OfferType.getType(offerType);
            }
            if (pairCurrency != null) {
                pairCur = DexCurrencies.getType(pairCurrency);
            }
            if (isAvailableForNow) {
                currentTime = epochTime.getEpochTime();
            }
            if(!StringUtils.isBlank(accountIdStr)){
                accountId = Long.parseUnsignedLong(accountIdStr);
            }
            // if(status != null){
                offerStatus = OfferStatus.OPEN; //getType(status);
            // }
        } catch (Exception ex){
            // return Response.ok(JSON.toString(JSONResponses.ERROR_INCORRECT_REQUEST)).build();
            log.error("incorrect request data");
        }

        int firstIndex = 0;//ParameterParser.getFirstIndex(req);
        int lastIndex = Integer.MAX_VALUE;//0xFFFFFFFF;//ParameterParser.getLastIndex(req);
        int offset = firstIndex > 0 ? firstIndex : 0;
        int limit = Integer.MAX_VALUE - 1;//DbUtils.calculateLimit(firstIndex, lastIndex);

        log.debug("args dump, type: {}, currentTime: {}, pairCur: {}, accountId: {}, offerStatus: {}, offset: {}, limit: {}", type, currentTime, pairCur, accountId, offerStatus, offset, limit );        

        DexOfferDBRequest dexOfferDBRequest = new DexOfferDBRequest(type, currentTime, DexCurrencies.APL, pairCur, accountId, offerStatus, null, null, offset, limit);
        List<DexOffer> offers = dexService.getOffers(dexOfferDBRequest);
        
        log.debug("got offers: " + offers.size() );
        
        // DexOffer match = offers.
        
        if (offers.size() >= 1) {
            DexOffer match = offers.get(0);
            DexOffer my = new DexOffer();
            my.setType(type);
            my.setAccountId(accountId);
            my.setOfferCurrency(DexCurrencies.APL);
            my.setOfferAmount(offerAmount);
            my.setPairCurrency(pairCur);
            my.setPairRate(pairRate);
            my.setStatus(offerStatus);
            my.setFromAddress(walletAddress);
            
            onOfferMatch(my, match);
            
        }
        
        
        


        
        
    }
    
}
