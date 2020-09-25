/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class BlockchainProducer {

    @Produces
    @Named("isPostConstruct")
    public boolean isPostConstruct() {
        return true;
    }

}
