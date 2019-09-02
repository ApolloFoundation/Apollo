/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Serhiy Lymar
 */

@Singleton
public class DexValidationServiceImpl implements IDexBasicServiceInterface, IDexValidator {
    
    private static final Logger log = LoggerFactory.getLogger(DexMatchingService.class);

    @Override
    public void initialize() {
        // placeholder
    }

    @Override
    public void deinitialize() {
        // placeholder
    }

    @Override
    public boolean validateAplSell(DexOffer myOffer, DexOffer counterOffer) {
        // placeholder
        return true; 
    }

    @Override
    public boolean validateAplBuy(DexOffer myOffer, DexOffer counterOffer) {
        // placeholder
        return true;
    }

    @Override
    public boolean validatEthSell(DexOffer myOffer, DexOffer counterOffer) {
        // placeholder
        return true;
    }

    @Override
    public boolean validateEthBuy(DexOffer myOffer, DexOffer counterOffer) {
        return true;
    }

    @Override
    public boolean validateStep1(DexOffer myOffer, DexOffer counterOffer) {
        return true;
    }

    @Override
    public boolean validateStep2(DexOffer myOffer, DexOffer counterOffer) {
        return true;
    }
    
}
