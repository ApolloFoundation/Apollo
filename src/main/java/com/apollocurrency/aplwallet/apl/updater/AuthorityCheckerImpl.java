/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import static com.apollocurrency.aplwallet.apl.updater.UpdaterConstants.FIRST_DECRYPTION_CERTIFICATE_PREFIX;
import static com.apollocurrency.aplwallet.apl.updater.UpdaterConstants.INTERMEDIATE_CERTIFICATE_NAME;
import static com.apollocurrency.aplwallet.apl.updater.UpdaterConstants.SECOND_DECRYPTION_CERTIFICATE_PREFIX;
import static com.apollocurrency.aplwallet.apl.updater.UpdaterUtil.readCertificate;
import static com.apollocurrency.aplwallet.apl.updater.UpdaterUtil.readCertificates;
import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.updater.downloader.DefaultDownloadExecutor;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.jar.JarFile;

public class AuthorityCheckerImpl implements AuthorityChecker {
    private static final Logger LOG = getLogger(AuthorityCheckerImpl.class);

    private Certificate caCertificate;
    private String caCertificateUrl;
    private String certificateSuffix = UpdaterConstants.CERTIFICATE_SUFFIX;
    private String[] certificatePrefixes = new String[] {FIRST_DECRYPTION_CERTIFICATE_PREFIX, SECOND_DECRYPTION_CERTIFICATE_PREFIX};
    private String intermediateCertificateName = INTERMEDIATE_CERTIFICATE_NAME;

    public AuthorityCheckerImpl(Certificate caCertificate) {
        this.caCertificate = caCertificate;

    }

    public AuthorityCheckerImpl(String caCertificateUrl, String certificateSuffix, String intermediateCertificateName, String... certificatePrefixes) {
        this.certificateSuffix = certificateSuffix;
        this.caCertificateUrl = caCertificateUrl;
        this.intermediateCertificateName = intermediateCertificateName;
        this.certificatePrefixes = certificatePrefixes;

    }
    public AuthorityCheckerImpl(Certificate rootCertificate, String certificateSuffix, String intermediateCertificateName,
                                String... certificatePrefixes) {
        this.certificateSuffix = certificateSuffix;
        this.caCertificate = rootCertificate;
        this.intermediateCertificateName = intermediateCertificateName;
        this.certificatePrefixes = certificatePrefixes;

    }

    public AuthorityCheckerImpl(String caCertificateUrl) {
        this.caCertificateUrl = caCertificateUrl;
    }

    public String getCertificateSuffix() {
        return certificateSuffix;
    }

    public void setCertificateSuffix(String certificateSuffix) {
        this.certificateSuffix = certificateSuffix;
    }

    public String[] getCertificatePrefixes() {
        return certificatePrefixes;
    }

    public void setCertificatePrefixes(String[] certificatePrefixes) {
        this.certificatePrefixes = certificatePrefixes;
    }

    @Override
    public boolean verifyCertificates(String certificateDirectory) {
        try {
            Set<Certificate> ordinaryCertificates = readCertificates(certificateDirectory, certificateSuffix, certificatePrefixes);
            Certificate intermediateCertificate = readCertificate(certificateDirectory + "/" + intermediateCertificateName);
            if (intermediateCertificate == null) {
                return false;
            }
            for (Certificate certificate : ordinaryCertificates) {
                certificate.verify(intermediateCertificate.getPublicKey());
            }
            if (caCertificate == null) {
                Path path = downloadCACertificate(caCertificateUrl);
                caCertificate = readCertificate(path);
            }
            intermediateCertificate.verify(caCertificate.getPublicKey());
            return true;
        }
        catch (CertificateException | IOException | URISyntaxException e) {
            LOG.error("Unable to read or load certificate", e);
        }
        catch (NoSuchAlgorithmException | SignatureException | NoSuchProviderException | InvalidKeyException e) {
            LOG.error("Unable to verify certificate signature", e);
        }
        return false;
    }

    private Path downloadCACertificate(String caCertificateUrl) throws IOException {
        DefaultDownloadExecutor defaultDownloadExecutor = new DefaultDownloadExecutor("ca", "caCertificate");
        return defaultDownloadExecutor.download(caCertificateUrl);
    }

    @Override
    public void verifyJarSignature(Certificate certificate, Path jarFilePath) throws IOException {
        try (JarFile jar = new JarFile(jarFilePath.toFile())) {
            JarVerifier verifier = new JarVerifier(jar);
            verifier.verify((X509Certificate) certificate);
        }
    }

    @Override
    public boolean verifyJarSignature(String certificateDirectory, Path jarFilePath) {
        Set<Certificate> ordinaryCertificates = readCertificates(certificateDirectory, certificateSuffix, certificatePrefixes);
        for (Certificate certificate : ordinaryCertificates) {
            try {
                verifyJarSignature(certificate, jarFilePath);
                return true;
            }
            catch (SecurityException | IOException e) {
                LOG.debug("Certificate is not appropriate." + UpdaterUtil.getStringRepresentation(certificate));
            }
        }
        return false;
    }
}
