/*
 * Copyright © 2017-2018 Apollo Foundation
 */

package apl.updater;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.*;
import java.util.stream.Collectors;

public class UpdaterUtil {
    static Set<CertificatePair> buildCertificatePairs(String certificateDirectory) throws IOException, CertificateException {
        Set<CertificatePair> certificatePairs = new HashSet<>();
        Path directory = Paths.get(certificateDirectory);
        Set<Certificate> firstDecryptionCertificates = readCertificates(findFiles(directory, UpdaterConstants.FIRST_DECRYPTION_CERTIFICATE_PREFIX, UpdaterConstants.CERTIFICATE_SUFFIX));
        Set<Certificate> secondDecryptionCertificates = readCertificates(findFiles(directory, UpdaterConstants.SECOND_DECRYPTION_CERTIFICATE_PREFIX, UpdaterConstants.CERTIFICATE_SUFFIX));
        for (Certificate firstCertificate : firstDecryptionCertificates) {
            for (Certificate secondCertificate : secondDecryptionCertificates) {
                certificatePairs.add(new CertificatePair(firstCertificate, secondCertificate));
            }
        }
        return certificatePairs;
    }

    static Set<Certificate> readCertificates(Set<Path> certificateFilesPaths) throws CertificateException, IOException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Iterator<Path> iterator = certificateFilesPaths.iterator();
        Set<Certificate> certificates = new HashSet<>();
        while (iterator.hasNext()) {
            Path certificateFilePath = iterator.next();
            certificates.add(cf.generateCertificate(Files.newInputStream(certificateFilePath)));
        }
        return certificates;
    }

    static Certificate readCertificate(Path certificateFilePath) throws CertificateException, IOException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return cf.generateCertificate(Files.newInputStream(certificateFilePath));
    }

    static Set<Certificate> readCertificates(Path directory, String prefix, String suffix) throws CertificateException, IOException {
        return readCertificates(findFiles(directory, prefix, suffix));
    }

    static Set<Certificate> readCertificates(Path directory, String suffix, String... prefixes) throws CertificateException, IOException {
        return readCertificates(findFiles(directory, suffix, prefixes));
    }


    static Set<Path> findFiles(Path directory, String prefix, String suffix) throws IOException {
        return Files.walk(directory, 1).filter(filePath -> filePath.getFileName().toString().endsWith(suffix) && filePath.getFileName().toString().startsWith(prefix)).collect(Collectors.toSet());
    }

    static Set<Path> findFiles(Path directory, String suffix, String... prefixes) throws IOException {
        return Files.walk(directory, 1)
                .filter(filePath -> {
                    String fileName = filePath.getFileName().toString();
                    return fileName.endsWith(suffix) && Arrays.stream(prefixes).anyMatch(fileName::startsWith);
                })
                .collect(Collectors.toSet());
    }

    private UpdaterUtil() {
    }

    static class CertificatePair {
        private Certificate firstCertificate;
        private Certificate secondCertificate;

        @Override
        public String toString() {
            return "CertificatePair{" +
                    "firstCertificate=" + firstCertificate +
                    ", secondCertificate=" + secondCertificate +
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
}
