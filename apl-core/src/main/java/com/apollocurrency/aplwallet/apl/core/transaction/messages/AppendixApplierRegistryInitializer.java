/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class AppendixApplierRegistryInitializer {
    @Inject
    AppendixApplierRegistry appendixApplierRegistry;
    @Inject
    Instance<AppendixApplier<?>> instances;

    @PostConstruct
    void init() {
        appendixApplierRegistry.init(instances);
    }
}
