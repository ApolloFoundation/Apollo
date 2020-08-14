/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TransactionTypesInitializer {
    @Inject
    Instance<TransactionType> typeInstances;
    @Inject
    CachedTransactionTypeFactory factory;

    @PostConstruct
    public void initTypes() {

        factory.init(typeInstances);

    }
}
