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
    

    boolean validateAplSell( DexOffer myOffer, DexOffer counterOffer );

    boolean validateAplBuy( DexOffer myOffer, DexOffer counterOffer );
    
    boolean validatEthSell( DexOffer myOffer, DexOffer counterOffer );
 
    boolean validateEthBuy( DexOffer myOffer, DexOffer counterOffer );
    
    
    boolean validateOfferBuyAplEth(DexOffer myOffer, DexOffer hisOffer);

    /**
     * currency-specific validation (Ethereum)
     * @param DexOffer  myOffer - created offer to validate
     * @param DexOffer  hisOffer - matched offer
    */     
    boolean validateOfferSellAplEth(DexOffer myOffer, DexOffer hisOffer);

    /**
     * currency-specific validation (Pax)
     * @param DexOffer  myOffer - created offer to validate
     * @param DexOffer  hisOffer - matched offer
    */ 
    boolean validateOfferBuyAplPax(DexOffer myOffer, DexOffer hisOffer);

    /**
     * currency-specific validation (Pax)
     * @param DexOffer  myOffer - created offer to validate
     * @param DexOffer  hisOffer - matched offer
    */ 
    boolean validateOfferSellAplPax( DexOffer myOffer, DexOffer hisOffer);
    
}
