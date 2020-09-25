/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.testutil;

import javax.enterprise.inject.Produces;
import javax.inject.Named;

public class BlockchainProducerUnitTests {

    @Produces
    @Named("isPostConstruct")
    public boolean isPostConstruct() {
        return false; // UNIT TEST version !
    }

}
