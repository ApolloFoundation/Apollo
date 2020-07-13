package com.apollocurrency.aplwallet.apl.core.transaction.messages.update;

import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.util.cert.ApolloCertificate;
import io.firstbridge.cryptolib.FBCryptoFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Singleton
public class CertificateMemoryStore {
    private CertificateLoader loader;
    private Map<BigInteger, ApolloCertificate> certificates = new HashMap<>();
    private final URL caCertUrl;

    @Inject
    public CertificateMemoryStore(@Property("updater.ca.cert-url") String caCertUrl,  CertificateLoader loader) throws MalformedURLException {
        this.loader = loader;
        this.caCertUrl = new URL(caCertUrl);
    }

    @PostConstruct
    void init() throws IOException {
        List<ApolloCertificate> all = loader.loadAll();
        X509Certificate rootCert = FBCryptoFactory.createDefault().getKeyReader().readX509CertPEMorDER(caCertUrl.openStream());
        for (ApolloCertificate apolloCertificate : all) {
            if (!apolloCertificate.verify(rootCert)) {
                throw new IllegalStateException("Certificate is not valid, ca signature verification failed for " + apolloCertificate);
            }
            if (apolloCertificate.isValid(new Date())) {
                throw new IllegalStateException("Certificate is out of valid time range: " + apolloCertificate);
            }
            certificates.put(apolloCertificate.getSerial(), apolloCertificate);
        }
    }

    public ApolloCertificate getBySn(BigInteger sn) {
        return certificates.get(sn);
    }
}
