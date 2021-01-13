/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

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