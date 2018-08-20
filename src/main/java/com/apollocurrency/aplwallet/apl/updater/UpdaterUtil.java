/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

public class UpdaterUtil {
    static Set<CertificatePair> buildCertificatePairs(String certificateDirectory) throws IOException, CertificateException, URISyntaxException {
        Set<CertificatePair> certificatePairs = new HashSet<>();
        Set<Certificate> firstDecryptionCertificates = readCertificates(findFiles(certificateDirectory, UpdaterConstants.SECOND_DECRYPTION_CERTIFICATE_PREFIX, UpdaterConstants.CERTIFICATE_SUFFIX));
        Set<Certificate> secondDecryptionCertificates = readCertificates(findFiles(certificateDirectory, UpdaterConstants.FIRST_DECRYPTION_CERTIFICATE_PREFIX, UpdaterConstants.CERTIFICATE_SUFFIX));
        for (Certificate firstCertificate : firstDecryptionCertificates) {
            for (Certificate secondCertificate : secondDecryptionCertificates) {
                certificatePairs.add(new CertificatePair(firstCertificate, secondCertificate));
            }
        }
        return certificatePairs;
    }

    static Set<Certificate> readCertificates(Set<Path> certificateFilesPaths) throws CertificateException, IOException, URISyntaxException {
        Iterator<Path> iterator = certificateFilesPaths.iterator();
        Set<Certificate> certificates = new HashSet<>();
        while (iterator.hasNext()) {
            Path certificateFilePath = iterator.next();
            certificates.add(readCertificate(certificateFilePath));
        }
        return certificates;
    }

    static Certificate readCertificate(String certificateFileName) throws CertificateException, IOException, URISyntaxException {
        Path certificateFilePath = loadResourcePath(certificateFileName);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return cf.generateCertificate(Files.newInputStream(certificateFilePath));
    }

    static Certificate readCertificate(Path certificatePath) throws CertificateException, IOException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return cf.generateCertificate(Files.newInputStream(certificatePath));
    }
    static String getStringRepresentation(Certificate cert) {
        return ((cert instanceof X509Certificate) ?
                ((X509Certificate) cert).getSubjectX500Principal().toString() : cert.toString());
    }

    static Set<Certificate> readCertificates(String directory, String prefix, String suffix) throws CertificateException, IOException, URISyntaxException {
        return readCertificates(findFiles(directory, prefix, suffix));
    }

    static Set<Certificate> readCertificates(String directory, String suffix, String... prefixes) throws CertificateException, IOException, URISyntaxException {
        return readCertificates(findFiles(directory, suffix, prefixes));
    }

    static Set<Path> findFiles(String directory, String prefix, String suffix) throws IOException, URISyntaxException {
        Path directoryPath = loadResourcePath(directory);
        return Files.walk(directoryPath, 1)
                .filter(filePath ->
                        filePath.getFileName().toString().endsWith(suffix) &&
                        filePath.getFileName().toString().startsWith(prefix))
                .collect(Collectors.toSet());
    }

    static Set<Path> findFiles(String directory, String suffix, String... prefixes) throws URISyntaxException, IOException {
        Path directoryPath = loadResourcePath(directory);
        return Files.walk(directoryPath, 1)
                .filter(filePath -> {
                    String fileName = filePath.getFileName().toString();
                    return fileName.endsWith(suffix)
                                    && Arrays.stream(prefixes).anyMatch(fileName::startsWith);
                })
                .collect(Collectors.toSet());
    }
    private UpdaterUtil(){}

    static class CertificatePair {
        private Certificate firstCertificate;
        private Certificate secondCertificate;

        @Override
        public String toString() {
            return "CertificatePair{" +
                    "firstCertificate=" + getStringRepresentation(firstCertificate) +
                    ", secondCertificate=" + getStringRepresentation(secondCertificate) +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CertificatePair)) return false;
            CertificatePair that = (CertificatePair) o;
            return Objects.equals(firstCertificate, that.firstCertificate) &&
                    Objects.equals(secondCertificate, that.secondCertificate);
        }

        @Override
        public int hashCode() {

            return Objects.hash(firstCertificate, secondCertificate);
        }

        public Certificate getFirstCertificate() {
            return firstCertificate;
        }

        public void setFirstCertificate(Certificate firstCertificate) {
            this.firstCertificate = firstCertificate;
        }

        public Certificate getSecondCertificate() {
            return secondCertificate;
        }

        public void setSecondCertificate(Certificate secondCertificate) {
            this.secondCertificate = secondCertificate;
        }

        public CertificatePair() {
        }

        public CertificatePair(Certificate firstCertificate, Certificate secondCertificate) {
            this.firstCertificate = firstCertificate;
            this.secondCertificate = secondCertificate;
        }
    }

    public static File loadResource(String fileName) {
        try {
            return new File(RSAUtil.class.getClassLoader().getResource(fileName).toURI());
        }
        catch (Exception e) {
            File file = Paths.get(fileName).toFile();
            if (file.exists()) {
                return file;
            } else {
                throw new RuntimeException("Cannot load resource " + fileName, e);
            }
        }
    }

    public static Path loadResourcePath(String fileName) throws URISyntaxException {
        return loadResource(fileName).toPath();
    }
}
