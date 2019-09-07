/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;

/**
 *
 * @author Serhiy Lymar
 */

public interface IDexMatcherInterface {
     /** 
     * Initialization routines
     */
     void initialize();
   
    /** 
     * Deinitialize the scope of internals
     */
    void deinitialize();

    DexOffer findCounterOffer(DexOffer createdOffer);
}
