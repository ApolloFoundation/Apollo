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
                    LOG.debug("Derytpted URL:", decryptedUrl);
                    LOG.debug("URL Pattern:", urlPattern.toString());
                    if (urlPattern.matcher(decryptedUrl).matches()) {
                        LOG.info("Decrypted url '{}' using: {} ", decryptedUrl, pair);
                        return decryptedUrl;
                    } else {
                        LOG.error("Decrypted url '{}' does not match pattern '{}'",decryptedUrl, urlPattern );
                    }
                } catch (GeneralSecurityException e) {
                    // BadPaddingException in case of incorrect certificate pair applied for decryption
                    LOG.info("Unable to decrypt using: {}, ex: {}", pair, e.toString());
                }
            }
        }
        return null;
    }
}
