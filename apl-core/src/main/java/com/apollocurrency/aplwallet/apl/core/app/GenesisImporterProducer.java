/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class GenesisImporterProducer {

    @Produces
    @Named("genesisParametersLocation")
    public String genesisParametersLocation() {
        return "data/genesisParameters.json";
    }
}
