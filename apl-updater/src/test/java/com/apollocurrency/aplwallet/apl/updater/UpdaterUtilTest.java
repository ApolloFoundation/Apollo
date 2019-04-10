/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//import org.powermock.api.mockito.PowerMockito;

//TODO: Rewrite using mockito
@Disabled
public class UpdaterUtilTest {

    private final static String CERTIFICATE_MOCK_PREFIX = "CERTIFICATE_MOCK_";

    @Mock
    private CertificateFactory certificateFactoryMock;

    /**
     * Test UpdaterUtil.buildCertificatePairs(String certificateDirectory) method
     * @throws Exception
     */
    @Test
    public void testBuildCertificatePairs() throws Exception {
        String directory = "test-dir";
        String[] files = new String[]{"1_1.crt", "1_2.crt", "1_3.crt", "2_1.crt", "2_2.crt"};

        Path directoryPath = Paths.get(directory);

//        PowerMockito.spy(UpdaterUtil.class);
//        PowerMockito.doReturn(directoryPath).when(UpdaterUtil.class, "loadResourcePath", directory) ;
//        PowerMockito.mockStatic(Files.class);
//        PowerMockito.when(Files.walk(directoryPath, 1)).thenReturn(createPathStream(files), createPathStream(files));
//        PowerMockito.mockStatic(CertificateFactory.class);
//        PowerMockito.when(CertificateFactory.getInstance("X.509")).thenReturn(certificateFactoryMock);

        // create certificate mock for each filename
        createCertificateMocksForFiles(certificateFactoryMock, files);

        // Call tested method
        Set<CertificatePair> result = UpdaterUtil.buildCertificatePairs(directory, "1_", "2_", ".crt");

        assertNotNull(result);
        for(CertificatePair pair : result) {
            System.out.println("pair: [" + pair.getFirstCertificate().toString() + ", " + pair.getSecondCertificate().toString() + "]");
        }

        assertEquals(result.size(), 6);

        assertTrue(containsPair(result, "2_1.crt", "1_1.crt"));
        assertTrue(containsPair(result, "2_1.crt", "1_2.crt"));
        assertTrue(containsPair(result, "2_1.crt", "1_3.crt"));
        assertTrue(containsPair(result, "2_2.crt", "1_1.crt"));
        assertTrue(containsPair(result, "2_2.crt", "1_2.crt"));
        assertTrue(containsPair(result, "2_2.crt", "1_3.crt"));

    }

    /**
     * Test UpdaterUtil.readCertificates(Set<Path> certificateFilesPaths) method
     * @throws CertificateException
     * @throws IOException
     */
    @Test
    public void testReadCertificates() throws Exception {

        String[] files = new String[]{"cert1", "cert2", "cert3"};

//        PowerMockito.mockStatic(Files.class);
//
//        PowerMockito.mockStatic(CertificateFactory.class);
//        PowerMockito.when(CertificateFactory.getInstance("X.509")).thenReturn(certificateFactoryMock);

        // create certificate mock for each filename
        createCertificateMocksForFiles(certificateFactoryMock, files);

        // Call tested method
        Set<Certificate> result = UpdaterUtil.readCertificates(createPathStream(files).collect(Collectors.toSet()));

        assertNotNull(result);
        assertEquals(result.size(), files.length);

        HashSet<String> filenames = new HashSet<>(Arrays.asList(files));
        // Assert that for each filename a correspondent certificate was created
        for(Certificate certificate : result) {
            assertTrue(filenames.contains(certificate.toString().replace(CERTIFICATE_MOCK_PREFIX, "")));
        }

    }

    /**
     * Create certificate mock for each filename.
     * Used to mock dependencies of UpdaterUtil.readCertificates(Set<Path> certificateFilesPaths) method
     * @param certificateFactoryMock
     * @param files
     * @throws IOException
     * @throws CertificateException
     */
    private static void createCertificateMocksForFiles(CertificateFactory certificateFactoryMock, String[] files) throws IOException, CertificateException {
        for(String filename : files) {
            InputStream inputStreamMock = Mockito.mock(InputStream.class);
            Certificate certificateMock = Mockito.mock(Certificate.class);

//            PowerMockito.when(Files.newInputStream(Paths.get(filename))).thenReturn(inputStreamMock);

            Mockito.when(certificateFactoryMock.generateCertificate(inputStreamMock)).thenReturn(certificateMock);
            Mockito.when(certificateMock.toString()).thenReturn(CERTIFICATE_MOCK_PREFIX + filename);
        }
    }

    /**
     * convert String[] to Stream<Path>
     * @param filenames
     * @return
     */
    private static Stream<Path> createPathStream(String[] filenames) {
        return Arrays.stream(filenames).map(filename -> Paths.get(filename));
    }

    /**
     * Simple iterate through result not to make filename to mock-cert mapping for better readability
     * @param pairs
     * @param first
     * @param second
     * @return
     */
    private static boolean containsPair(Set<CertificatePair> pairs, String first, String second) {
        for(CertificatePair pair : pairs) {
            if( pair.getFirstCertificate().toString().equals(CERTIFICATE_MOCK_PREFIX + first) &&
                pair.getSecondCertificate().toString().equals(CERTIFICATE_MOCK_PREFIX + second)) {
                return true;
            }
        }
        return false;
    }

}
