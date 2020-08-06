/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

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
