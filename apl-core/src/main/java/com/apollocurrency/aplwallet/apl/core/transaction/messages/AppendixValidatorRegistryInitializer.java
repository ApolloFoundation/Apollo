/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class AppendixValidatorRegistryInitializer {
    @Inject
    AppendixValidatorRegistry registry;
    @Inject
    Instance<AppendixValidator<?>> instances;

    @PostConstruct
    void init() {
        registry.init(instances);
    }
}