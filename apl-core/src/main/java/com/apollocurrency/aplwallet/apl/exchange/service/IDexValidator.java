/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;

/**
 *
 * @author Serhiy Lymar
 */
public interface IDexValidator {
    
     /** 
     * Initialization routines
     */
     void initialize();
     
    /** 
     * Deinitialize the scope of internals
     */
    void deinitialize();
    
    /**
     * currency-specific validation (Ethereum)
     * @param DexOffer  myOffer - created offer to validate
     * @param DexOffer  hisOffer - matched offer
     * @return 1 if success, -1 if 
    */     
    int validateOfferBuyAplEth(DexOffer myOffer, DexOffer hisOffer);

    /**
     * currency-specific validation (Ethereum)
     * @param DexOffer  myOffer - created offer to validate
     * @param DexOffer  hisOffer - matched offer
     * @return 1 if success, -1 if 
    */     
    int validateOfferSellAplEth(DexOffer myOffer, DexOffer hisOffer);

    /**
     * currency-specific validation (Pax)
     * @param DexOffer  myOffer - created offer to validate
     * @param DexOffer  hisOffer - matched offer
    */ 
    int validateOfferBuyAplPax(DexOffer myOffer, DexOffer hisOffer);

    /**
     * currency-specific validation (Pax)
     * @param DexOffer  myOffer - created offer to validate
     * @param DexOffer  hisOffer - matched offer
    */ 
    int validateOfferSellAplPax( DexOffer myOffer, DexOffer hisOffer);
    
}
