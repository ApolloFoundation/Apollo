/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages.update;

import com.apollocurrency.aplwallet.apl.util.Constants;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@Singleton
public class CertificateLoaderProducer {
    @Produces
    @Singleton
    public CertificateLoader loader() {
        return new CertificateLoader(this.getClass(), Constants.VERSION);
    }
}
