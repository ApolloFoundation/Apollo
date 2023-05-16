/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.model.dex.DexOrder;

/**
 * @author Serhiy Lymar
 */
public interface IDexMatcherInterface {
    /**
     * Start transport interaction service
     */
    void initialize();

    /**
     * Stop transport interaction service
     */
    void deinitialize();

    DexOrder findCounterOffer(DexOrder createdOffer);
}
