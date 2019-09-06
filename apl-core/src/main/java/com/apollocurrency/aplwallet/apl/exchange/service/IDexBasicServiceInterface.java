/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;

/**
 *
 * @author Serhiy Lymar
 */

public interface IDexBasicServiceInterface {
     /** 
     * Start transport interaction service
     */
     void initialize();
   
    /** 
     * Stop transport interaction service
     */
    void deinitialize();

    DexOffer findCounterOffer(DexOffer createdOffer);
}
