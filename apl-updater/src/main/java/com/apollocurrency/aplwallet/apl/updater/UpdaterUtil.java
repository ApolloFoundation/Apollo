/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.util.DoubleByteArrayTuple;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.Vetoed;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//TODO: how to read from resources
//       ClassLoader classloader = Thread.currentThread().getContextClassLoader();
//        try (InputStream is = classloader.getResourceAsStream("conf/updater.properties")) {
//            // rerad from is;
//        }

@Vetoed
public class UpdaterUtil {
    private static final Logger LOG = LoggerFactory.getLogger(UpdaterUtil.class);

    static Set<CertificatePair> certificatePairs = new HashSet<>();
    static Set<Certificate> certificates = new HashSet<>();

    private UpdaterUtil() {
    }

    public static void init(boolean useDebugCerts) {
        try {
            if (!useDebugCerts) {

                Certificate certificate1_1 = readCertificate("certs/1_1.crt");
                Certificate certificate1_2 = readCertificate("certs/1_2.crt");
                Certificate certificate1_3 = readCertificate("certs/1_3.crt");
                Certificate certificate1_4 = readCertificate("certs/1_4.crt");
                Certificate certificate2_1 = readCertificate("certs/2_1.crt");
                Certificate certificate2_2 = readCertificate("certs/2_2.crt");
                Certificate certificate2_3 = readCertificate("certs/2_3.crt");
                certificates.add(certificate1_1);
                certificates.add(certificate1_2);
                certificates.add(certificate1_3);
                certificates.add(certificate1_4);
                certificates.add(certificate2_1);
                certificates.add(certificate2_2);
                certificates.add(certificate2_3);
                certificatePairs.add(new CertificatePair(certificate2_1, certificate1_1));
                certificatePairs.add(new CertificatePair(certificate2_1, certificate1_2));
                certificatePairs.add(new CertificatePair(certificate2_1, certificate1_3));
                certificatePairs.add(new CertificatePair(certificate2_1, certificate1_4));
                certificatePairs.add(new CertificatePair(certificate2_2, certificate1_1));
                certificatePairs.add(new CertificatePair(certificate2_2, certificate1_2));
                certificatePairs.add(new CertificatePair(certificate2_2, certificate1_3));
                certificatePairs.add(new CertificatePair(certificate2_2, certificate1_4));
                certificatePairs.add(new CertificatePair(certificate2_3, certificate1_1));
                certificatePairs.add(new CertificatePair(certificate2_3, certificate1_2));
                certificatePairs.add(new CertificatePair(certificate2_3, certificate1_3));
                certificatePairs.add(new CertificatePair(certificate2_3, certificate1_4));
            } else {
                Certificate certificate1_1 = readCertificate("debug-certs/1_1.crt");
                Certificate certificate2_1 = readCertificate("debug-certs/2_1.crt");
                certificates.add(certificate1_1);
                certificates.add(certificate2_1);
                certificatePairs.add(new CertificatePair(certificate2_1, certificate1_1));
            }
        } catch (CertificateException | IOException | URISyntaxException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static Set<CertificatePair> buildCertificatePairs(String certificateDirectory, String firstCertificatePrefix,
                                                             String secondCertificatePrefix, String certificateSuffix) throws IOException,
        CertificateException {
//        Set<CertificatePair> certificatePairs = new HashSet<>();
//        Set<Certificate> firstDecryptionCertificates = readCertificates(findFiles(certificateDirectory,
//                secondCertificatePrefix, certificateSuffix));
//        Set<Certificate> secondDecryptionCertificates = readCertificates(findFiles(certificateDirectory,
//                firstCertificatePrefix, certificateSuffix));
//        for (Certificate firstCertificate : firstDecryptionCertificates) {
//            for (Certificate secondCertificate : secondDecryptionCertificates) {
//                certificatePairs.add(new CertificatePair(firstCertificate, secondCertificate));
//            }
//        }
        return certificatePairs;
    }

    public static Set<Certificate> readCertificates(Set<Path> certificateFilesPaths) throws CertificateException, IOException {
//        Iterator<Path> iterator = certificateFilesPaths.iterator();
//        Set<Certificate> certificates = new HashSet<>();
//        while (iterator.hasNext()) {
//            Path certificateFilePath = iterator.next();
//            certificates.add(readCertificate(certificateFilePath));
//        }
        return certificates;
    }

    public static Certificate readCertificate(String certificateFileName) throws CertificateException, IOException, URISyntaxException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        URL resource = getResource(certificateFileName);
        LOG.debug("Loaded resource: {}", resource);
        return cf.generateCertificate(resource.openStream());
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
        return certificates;
        //        try {
//            return readCertificates(findFiles(directory, suffix, prefixes));
//        }
//        catch (CertificateException | IOException  e) {
//            throw new RuntimeException(e.toString(), e);
//        }
    }

    public static Set<Path> findFiles(Path directory, String prefix, String suffix) throws IOException {

        return Files.walk(directory, 1)
            .filter(filePath ->
                filePath.getFileName().toString().endsWith(suffix) &&
                    filePath.getFileName().toString().startsWith(prefix))
            .collect(Collectors.toSet());
    }

    public static Set<Path> findFiles(String directory, String suffix, String... prefixes) {
        return walk(directory)
            .filter(filePath -> {
                String fileName = filePath.getFileName().toString();
                return fileName.endsWith(suffix)
                    && Arrays.stream(prefixes).anyMatch(fileName::startsWith);
            })
            .collect(Collectors.toSet());
    }

    public static DoubleByteArrayTuple split(byte[] arr) {
        byte[] first = ArrayUtils.subarray(arr, 0, arr.length / 2);
        byte[] second = ArrayUtils.subarray(arr, arr.length / 2, arr.length);
        return new DoubleByteArrayTuple(first,
            second);
    }

    public static Stream<Path> walk(String path) {
        try {
            URL url = getResource(path);
            if ("jar".equals(url.toURI().getScheme())) {
                return safeWalkJar(path, url.toURI());
            } else {
                return Files.walk(Paths.get(url.toURI()));
            }
        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static URL getResource(String resource) {
        ClassLoader contextClassLoader = UpdaterUtil.class.getClassLoader();
        return contextClassLoader.getResource(resource);
    }

    private static Stream<Path> safeWalkJar(String path, URI uri) throws Exception {
        try (FileSystem fs = getFileSystem(uri)) {
            return Files.walk(fs.getPath(path));
        }
    }


    private static FileSystem getFileSystem(URI uri) throws IOException {

        try {
            return FileSystems.getFileSystem(uri);
        } catch (FileSystemNotFoundException e) {
            return FileSystems.newFileSystem(uri, Collections.<String, String>emptyMap());
        }
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
