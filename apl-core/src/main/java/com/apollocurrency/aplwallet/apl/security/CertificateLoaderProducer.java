package com.apollocurrency.aplwallet.apl.security;

import com.apollocurrency.aplwallet.apl.util.Constants;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

@Singleton
public class CertificateLoaderProducer {
    @Produces
    @Singleton
    public CertificateLoader loader() {
        return new CertificateLoader(this.getClass(), Constants.VERSION);
    }
}
