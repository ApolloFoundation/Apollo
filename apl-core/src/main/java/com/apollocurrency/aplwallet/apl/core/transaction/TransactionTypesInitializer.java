/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

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
