package com.apollocurrency.aplwallet.apl.core.transaction.messages.update;

import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.util.cert.ApolloCertificate;
import io.firstbridge.cryptolib.CryptoFactory;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Singleton
public class CertificateMemoryStore {

    private CertificateLoader loader;
    private Map<BigInteger, ApolloCertificate> certificates = new HashMap<>();
    private final URL caCertUrl;

    @Inject
    public CertificateMemoryStore(@Property("updater.ca.cert-url") String caCertUrl, CertificateLoader loader) throws MalformedURLException {
        this.loader = Objects.requireNonNull(loader);
        String notNullCertUrl = Objects.requireNonNull(caCertUrl);
        this.caCertUrl = new URL(notNullCertUrl);
    }

    @PostConstruct
    void init() {
        List<ApolloCertificate> all = null;
        try {
            all = loader.loadAll();
        } catch (Exception e) {
            log.debug("Error loading all certificates !", e);
        }
        X509Certificate rootCert = null;
        try {
            rootCert = CryptoFactory.createDefault().getKeyReader().readX509CertPEMorDER(caCertUrl.openStream());
        } catch (Exception e) {
            log.debug("Error readX509 CertPEMorDER", e);
        }
        if (all != null) {
            for (ApolloCertificate apolloCertificate : all) {
                if (rootCert != null && !apolloCertificate.verify(rootCert)) {
                    throw new IllegalStateException("Certificate is not valid, ca signature verification failed for " + apolloCertificate);
                }
                if (apolloCertificate.isValid(new Date())) {
                    throw new IllegalStateException("Certificate is out of valid time range: " + apolloCertificate);
                }
                certificates.put(apolloCertificate.getSerial(), apolloCertificate);
            }
        }
    }

    public ApolloCertificate getBySn(BigInteger sn) {
        return certificates.get(sn);
    }
}
