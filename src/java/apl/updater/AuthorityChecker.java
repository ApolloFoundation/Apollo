/*
 * Copyright © 2017-2018 Apollo Foundation
 */

package apl.updater;

import apl.util.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.jar.JarFile;

import static apl.updater.UpdaterUtil.readCertificate;
import static apl.updater.UpdaterUtil.readCertificates;

public class AuthorityChecker {
    private Downloader downloader = Downloader.getInstance();

    private static class AuthorityCheckerHolder {
        private static final AuthorityChecker INSTANCE = new AuthorityChecker();
    }

    private AuthorityChecker() {
    }

    public static AuthorityChecker getInstance() {
        return AuthorityCheckerHolder.INSTANCE;
    }

    public boolean verifyCertificates(String certificateDirectory) {
        Path directoryPath = Paths.get(certificateDirectory);
        try {
            Set<Certificate> ordinaryCertificates = readCertificates(directoryPath, UpdaterConstants.CERTIFICATE_SUFFIX, UpdaterConstants.FIRST_DECRYPTION_CERTIFICATE_PREFIX, UpdaterConstants.SECOND_DECRYPTION_CERTIFICATE_PREFIX);
            Certificate intermediateCertificate = readCertificate(directoryPath.resolve(UpdaterConstants.INTERMEDIATE_CERTIFICATE_NAME));
            for (Certificate certificate : ordinaryCertificates) {
                certificate.verify(intermediateCertificate.getPublicKey());
            }
            Path path = downloadCACertificate();
            Certificate caCertificate = readCertificate(path);
            intermediateCertificate.verify(caCertificate.getPublicKey());
            return true;
        } catch (CertificateException | IOException e) {
            Logger.logErrorMessage("Unable to read or load certificate", e);
        } catch (NoSuchAlgorithmException | SignatureException | NoSuchProviderException | InvalidKeyException e) {
            Logger.logErrorMessage("Unable to verify certificate signature", e);
        }
        return false;
    }

    private Path downloadCACertificate() throws IOException {
        return downloader.downloadAttempt(UpdaterConstants.CA_CERTIFICATE_URL, "", UpdaterConstants.CA_CERTIFICATE_NAME);
    }

    boolean verifyJarSignature(Certificate certificate, Path jarFilePath) throws IOException {
        JarFile jar = new JarFile(jarFilePath.toFile(), true);
        JarVerifier verifier = new JarVerifier(jar);
        verifier.verify((X509Certificate) certificate);
        return true;
    }
//uncomment for tests
//    public static void main(String[] args) throws CertificateException, IOException {
//        Path jar = Paths.get("E:/fb/signed1.jar");
//        Certificate certificate = UpdaterUtil.readCertificate(Paths.get("conf/certs/1_1.cert.pem"));
//        AuthorityChecker.getInstance().verifyJarSignature(certificate, jar);
//    }
}
