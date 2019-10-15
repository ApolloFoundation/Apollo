/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;



/**
 *
 * @author Serhiy Lymar
 */
public interface IDexValidator {
    
    
    /**
     * currency-specific validation (Ethereum)
     * @param DexOffer  myOffer - created offer to validate
     * @param DexOffer  hisOffer - matched offer
     * @return 1 if success, -1 if 
    */     
    int validateOfferBuyAplEth(DexOrder myOffer, DexOrder hisOffer);

    /**
     * currency-specific validation (Ethereum)
     * @param DexOffer  myOffer - created offer to validate
     * @param DexOffer  hisOffer - matched offer
     * @return 1 if success, -1 if 
    */     
    int validateOfferSellAplEth(DexOrder myOffer, DexOrder hisOffer);

    /**
     * currency-specific validation (Pax)
     * @param DexOffer  myOffer - created offer to validate
     * @param DexOffer  hisOffer - matched offer
    */ 
    int validateOfferBuyAplPax(DexOrder myOffer, DexOrder hisOffer);

    /**
     * currency-specific validation (Pax)
     * @param DexOffer  myOffer - created offer to validate
     * @param DexOffer  hisOffer - matched offer
    */ 
    int validateOfferSellAplPax( DexOrder myOffer, DexOrder hisOffer);
    
}
