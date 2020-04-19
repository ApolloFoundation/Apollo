/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.updater.decryption.DoubleDecryptor;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Set;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;

public class SimpleUrlExtractor implements UrlExtractor {
    private static final Logger LOG = getLogger(SimpleUrlExtractor.class);

    private String defaultCertificatesDirectory = UpdaterConstants.CERTIFICATE_DIRECTORY;
    private CertificatePairsProvider certificatePairsProvider;
    private Set<CertificatePair> certificatePairs;
    private DoubleDecryptor decryptor;

    public SimpleUrlExtractor(DoubleDecryptor decryptor, CertificatePairsProvider certificatePairsProvider) {
        this.decryptor = decryptor;
        this.certificatePairsProvider = certificatePairsProvider;
    }

    public SimpleUrlExtractor(DoubleDecryptor decryptor, Set<CertificatePair> certificatePairs) {
        this.decryptor = decryptor;
        this.certificatePairs = certificatePairs;
    }

    public SimpleUrlExtractor(DoubleDecryptor decryptor) {
        this.decryptor = decryptor;
        this.certificatePairsProvider = new FileSystemCertificatePairsProvider(defaultCertificatesDirectory);
    }


    @Override
    public String extract(byte[] encryptedUrlBytes, Pattern urlPattern) {
        Set<CertificatePair> certPairs = certificatePairs != null ? certificatePairs : certificatePairsProvider.getPairs();
        if (certPairs != null) {
            for (CertificatePair pair : certPairs) {
                try {
                    byte[] urlBytes = decryptor.decrypt(encryptedUrlBytes,
                        pair.getFirstCertificate().getPublicKey(),
                        pair.getSecondCertificate().getPublicKey()
                    );
                    String decryptedUrl = new String(urlBytes, StandardCharsets.UTF_8);
                    if (urlPattern.matcher(decryptedUrl).matches()) {
                        LOG.debug("Decrypted url using: " + pair);
                        return decryptedUrl;
                    }
                } catch (GeneralSecurityException e) {
                    LOG.info("Unable to decrypt using: {}", pair);
                }
            }
        }
        return null;
    }
}
