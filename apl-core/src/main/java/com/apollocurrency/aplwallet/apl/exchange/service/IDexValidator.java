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
    
    /**
     *
     * @param myOffer offer that has been created locally
     * @param counterOffer offer which is from the other side of the wire
     * @return
     */
    public boolean validateStep1(DexOffer myOffer, DexOffer counterOffer);
    
    /**
     *
     * @param myOffer offer that has been created locally
     * @param counterOffer offer which is from the other side of the wire
     * @return
     */
    public boolean validateStep2(DexOffer myOffer, DexOffer counterOffer);
    

    
}
