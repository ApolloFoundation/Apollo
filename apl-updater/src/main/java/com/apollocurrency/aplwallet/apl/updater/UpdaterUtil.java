/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.util.DoubleByteArrayTuple;
import com.apollocurrency.aplwallet.apl.updater.decryption.RSAUtil;

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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.apollocurrency.aplwallet.apl.updater.decryption.RSAUtil;
import javax.enterprise.inject.Vetoed;
import org.apache.commons.lang3.ArrayUtils;

@Vetoed
public class UpdaterUtil {
    public static Set<CertificatePair> buildCertificatePairs(String certificateDirectory, String firstCertificatePrefix,
     String secondCertificatePrefix, String certificateSuffix) {
        try {
            return buildCertificatePairs(loadResourcePath(certificateDirectory), firstCertificatePrefix, secondCertificatePrefix, certificateSuffix);
        }
        catch (IOException | CertificateException | URISyntaxException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
    public static Set<CertificatePair> buildCertificatePairs(Path certificateDirectory, String firstCertificatePrefix,
                                                             String secondCertificatePrefix, String certificateSuffix) throws IOException,
            CertificateException {
        Set<CertificatePair> certificatePairs = new HashSet<>();
        Set<Certificate> firstDecryptionCertificates = readCertificates(findFiles(certificateDirectory,
                secondCertificatePrefix, certificateSuffix));
        Set<Certificate> secondDecryptionCertificates = readCertificates(findFiles(certificateDirectory,
                firstCertificatePrefix, certificateSuffix));
        for (Certificate firstCertificate : firstDecryptionCertificates) {
            for (Certificate secondCertificate : secondDecryptionCertificates) {
                certificatePairs.add(new CertificatePair(firstCertificate, secondCertificate));
            }
        }
        return certificatePairs;
    }

    public static Set<Certificate> readCertificates(Set<Path> certificateFilesPaths) throws CertificateException, IOException {
        Iterator<Path> iterator = certificateFilesPaths.iterator();
        Set<Certificate> certificates = new HashSet<>();
        while (iterator.hasNext()) {
            Path certificateFilePath = iterator.next();
            certificates.add(readCertificate(certificateFilePath));
        }
        return certificates;
    }

    public static Certificate readCertificate(String certificateFileName) throws CertificateException, IOException, URISyntaxException {
        Path certificateFilePath = loadResourcePath(certificateFileName);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return cf.generateCertificate(Files.newInputStream(certificateFilePath));
    }

    public static Certificate readCertificate(Path certificatePath) throws CertificateException, IOException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return cf.generateCertificate(Files.newInputStream(certificatePath));
    }
    public static String getStringRepresentation(Certificate cert) {
        return ((cert instanceof X509Certificate) ?
                ((X509Certificate) cert).getSubjectX500Principal().toString() : cert.toString());
    }

    public static Set<Certificate> readCertificates(String directory, String prefix, String suffix) {
        return readCertificates(directory, suffix, prefix);
    }

    public static Set<Certificate> readCertificates(String directory, String suffix, String... prefixes) {
        try {
            return readCertificates(findFiles(directory, suffix, prefixes));
        }
        catch (CertificateException | IOException | URISyntaxException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static Set<Path> findFiles(String directory, String prefix, String suffix) throws IOException, URISyntaxException {
        Path directoryPath = loadResourcePath(directory);
        return findFiles(directoryPath, prefix, suffix);
    }
    public static Set<Path> findFiles(Path directory, String prefix, String suffix) throws IOException {

        return Files.walk(directory, 1)
                .filter(filePath ->
                        filePath.getFileName().toString().endsWith(suffix) &&
                                filePath.getFileName().toString().startsWith(prefix))
                .collect(Collectors.toSet());
    }


    public static Set<Path> findFiles(String directory, String suffix, String... prefixes) throws URISyntaxException, IOException {
        Path directoryPath = loadResourcePath(directory);
        return findFiles(directoryPath, suffix, prefixes);
    }

    public static Set<Path> findFiles(Path directory, String suffix, String... prefixes) throws IOException {
        return Files.walk(directory, 1)
                .filter(filePath -> {
                    String fileName = filePath.getFileName().toString();
                    return fileName.endsWith(suffix)
                            && Arrays.stream(prefixes).anyMatch(fileName::startsWith);
                })
                .collect(Collectors.toSet());
    }

    public static DoubleByteArrayTuple split(byte[] arr) {
        byte[] first = ArrayUtils.subarray(arr, 0,arr.length / 2);
        byte[] second = ArrayUtils.subarray(arr, arr.length / 2, arr.length);
        return new DoubleByteArrayTuple(first,
                second);
    }

    private UpdaterUtil(){}

    public static class CertificatePair {
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

    public static byte[] concatArrays(byte[] arr1, byte[] arr2) {
        int aLen = arr1.length;
        int bLen = arr2.length;
        byte[] result = new byte[aLen + bLen];

        System.arraycopy(arr1, 0, result, 0, aLen);
        System.arraycopy(arr2, 0, result, aLen, bLen);
        return result;
    }
}
