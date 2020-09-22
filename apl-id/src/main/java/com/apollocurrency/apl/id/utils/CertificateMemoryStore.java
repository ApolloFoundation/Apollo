package com.apollocurrency.apl.id.utils;

import com.apollocurrency.apl.id.cert.ExtCert;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Singleton
public class CertificateMemoryStore {

    private final CertificateLoader loader;
    private final Map<BigInteger, ExtCert> certificates = new HashMap<>();

    @Inject
    public CertificateMemoryStore(CertificateLoader loader) throws MalformedURLException {
        this.loader = Objects.requireNonNull(loader);
    }

    @PostConstruct
    void init() {
        List<ExtCert> all = null;
        try {
            all = loader.loadAll();
        } catch (IOException e) {
            log.debug("Error loading all certificates !", e);
        }
        ExtCert rootCert = loader.getCaCert();
        if(rootCert==null){
             throw new IllegalStateException("CA Certificate is not loaded!");                     
        }
        if (all != null) {
            for (ExtCert apolloCertificate : all) {
                if (!apolloCertificate.verify(rootCert.getCertificate())) {
                    throw new IllegalStateException("Certificate is not valid, ca signature verification failed for " + apolloCertificate);
                }
                if (apolloCertificate.isValid(new Date())) {
                    throw new IllegalStateException("Certificate is out of valid time range: " + apolloCertificate);
                }
                certificates.put(apolloCertificate.getActorId(), apolloCertificate);
            }
        }
    }

    public ExtCert getBySn(BigInteger sn) {
        return certificates.get(sn);
    }
}
