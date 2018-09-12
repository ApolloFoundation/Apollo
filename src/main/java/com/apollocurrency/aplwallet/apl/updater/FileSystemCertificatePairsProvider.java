/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import java.io.IOException;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.util.Set;

public class FileSystemCertificatePairsProvider implements CertificatePairsProvider {
    private Path certificateDir;
    private String firstCertificatePrefix = UpdaterConstants.FIRST_DECRYPTION_CERTIFICATE_PREFIX;
    private String certificateSuffix = UpdaterConstants.CERTIFICATE_SUFFIX;
    private String secondCertificatePrefix = UpdaterConstants.SECOND_DECRYPTION_CERTIFICATE_PREFIX;


    public FileSystemCertificatePairsProvider(Path certificateDir) {
        this.certificateDir = certificateDir;
    }

    public String getFirstCertificatePrefix() {
        return firstCertificatePrefix;
    }

    public void setFirstCertificatePrefix(String firstCertificatePrefix) {
        this.firstCertificatePrefix = firstCertificatePrefix;
    }

    public String getCertificateSuffix() {
        return certificateSuffix;
    }

    public void setCertificateSuffix(String certificateSuffix) {
        this.certificateSuffix = certificateSuffix;
    }

    public String getSecondCertificatePrefix() {
        return secondCertificatePrefix;
    }

    public void setSecondCertificatePrefix(String secondCertificatePrefix) {
        this.secondCertificatePrefix = secondCertificatePrefix;
    }

    @Override
    public Set<UpdaterUtil.CertificatePair> getPairs() {
        try {
            return UpdaterUtil.buildCertificatePairs(certificateDir, firstCertificatePrefix, secondCertificatePrefix, certificateSuffix);
        }
        catch (IOException | CertificateException e) {
            throw new RuntimeException("Unable to load certificate pairs from " + certificateDir.toAbsolutePath(), e);
        }
    }
}
