package com.apollocurrency.aplwallet.apl.security.id;

import com.apollocurrency.aplwallet.apl.util.Constants;
import io.firstbridge.identity.handler.CertificateLoader;
import io.firstbridge.identity.handler.CertificateLoaderImpl;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

@Singleton
public class CertificateLoaderProducer {
    @Produces
    @Singleton
    public CertificateLoader loader() {
        return new CertificateLoaderImpl();//this.getClass(), Constants.VERSION.toString());
    }
}
