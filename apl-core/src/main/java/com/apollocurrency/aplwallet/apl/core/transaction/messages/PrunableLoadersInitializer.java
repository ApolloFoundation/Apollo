/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class PrunableLoadersInitializer {
    @Inject
    Instance<PrunableLoader<?>> prunableLoaderInstances;

    @Inject
    PrunableLoadingService prunableLoadingService;

    @PostConstruct
    public void initPrunableLoaders() {
        prunableLoadingService.initLoaders(prunableLoaderInstances);
    }
}
