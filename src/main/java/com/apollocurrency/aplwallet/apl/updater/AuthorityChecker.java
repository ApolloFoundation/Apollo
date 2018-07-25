/*
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package com.apollocurrency.aplwallet.apl.updater;

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

import static com.apollocurrency.aplwallet.apl.updater.UpdaterConstants.*;
import static com.apollocurrency.aplwallet.apl.updater.UpdaterUtil.readCertificate;
import static com.apollocurrency.aplwallet.apl.updater.UpdaterUtil.readCertificates;
import static org.slf4j.LoggerFactory.getLogger;

public class AuthorityChecker {
    private static final Logger LOG = getLogger(AuthorityChecker.class);
    private static final DefaultDownloadExecutor downloader = new DefaultDownloadExecutor("ca", "caCertificate");

    private static class AuthorityCheckerHolder {
        private static final AuthorityChecker INSTANCE = new AuthorityChecker();
    }

    private AuthorityChecker() {}

    public static AuthorityChecker getInstance() {
        return AuthorityCheckerHolder.INSTANCE;
    }

    public boolean verifyCertificates(String certificateDirectory) {
        try {
            Set<Certificate> ordinaryCertificates = readCertificates(certificateDirectory, CERTIFICATE_SUFFIX, FIRST_DECRYPTION_CERTIFICATE_PREFIX, SECOND_DECRYPTION_CERTIFICATE_PREFIX);
            Certificate intermediateCertificate = readCertificate(certificateDirectory + "/" + INTERMEDIATE_CERTIFICATE_NAME);
            for (Certificate certificate : ordinaryCertificates) {
                certificate.verify(intermediateCertificate.getPublicKey());
            }
            Path path = downloadCACertificate();
            Certificate caCertificate = readCertificate(path);
            intermediateCertificate.verify(caCertificate.getPublicKey());
            return true;
        }
        catch (CertificateException |IOException | URISyntaxException e) {
            LOG.error("Unable to read or load certificate", e);
        }
        catch (NoSuchAlgorithmException | SignatureException | NoSuchProviderException | InvalidKeyException e) {
            LOG.error("Unable to verify certificate signature", e);
        }
        return false;
    }
    private Path downloadCACertificate() throws IOException {
        return downloader.download(UpdaterConstants.CA_CERTIFICATE_URL);
    }

    public void verifyJarSignature(Certificate certificate, Path jarFilePath) throws IOException {
        try(JarFile jar = new JarFile(jarFilePath.toFile())) {
            JarVerifier verifier = new JarVerifier(jar);
            verifier.verify((X509Certificate) certificate);
        }
    }
}
