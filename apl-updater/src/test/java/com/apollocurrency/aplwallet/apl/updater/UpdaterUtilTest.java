/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.slf4j.LoggerFactory.getLogger;


public class UpdaterUtilTest {
    private static final Logger log = getLogger(UpdaterUtilTest.class);

    @AfterEach
    void tearDown() {
        UpdaterUtil.certificates.clear();
        UpdaterUtil.certificatePairs.clear();
    }

    @Test
    public void testBuildCertificatePairs() throws Exception {
        UpdaterUtil.init(false);
        // Call tested method
        Set<CertificatePair> result = UpdaterUtil.buildCertificatePairs("any-dir",
            "1_", "2_", ".crt");

        assertNotNull(result);
        for (CertificatePair pair : result) {
            log.debug("pair: [{}, {}]", pair.getFirstCertificate().toString(), pair.getSecondCertificate().toString());
        }
        assertEquals(result.size(), 12);
        assertHasPair(result, "YL", "Denis Demut");
        assertHasPair(result, "YL", "Dzhyncharadze George");
        assertHasPair(result, "YL", "iAlexander");
        assertHasPair(result, "Rostyslav Golda", "Denis Demut");
        assertHasPair(result, "Rostyslav Golda", "Dzhyncharadze George");
        assertHasPair(result, "Rostyslav Golda", "iAlexander");
        assertHasPair(result, "Maksim Khabenko", "Denis Demut");
        assertHasPair(result, "Maksim Khabenko", "Dzhyncharadze George");
        assertHasPair(result, "Maksim Khabenko", "iAlexander");
    }

    @Test
    public void testBuildDebugCertificatePairs() throws Exception {
        UpdaterUtil.init(true);

        Set<CertificatePair> result = UpdaterUtil.buildCertificatePairs("any-dir",
            "1_", "2_", ".crt");

        assertNotNull(result);
        assertEquals(result.size(), 1);
        assertHasPair(result, "Andrii Boiarskyi", "Andrii Boiarskyi");
    }


    @Test
    public void testReadMainCertificates() throws IOException {

        // init certificates (from apl-updater/src/main/resources/certs)
        UpdaterUtil.init(false);

        // read certificates ignoring input parameters
        Set<Certificate> result = UpdaterUtil.readCertificates("any-dir", "any-prefix", "any-suffix");

        assertNotNull(result);
        assertEquals(7, result.size());

        // Assert that for each filename a correspondent certificate was created
        final Set<String> names = Set.of("Denis Demut", "YL", "Rostyslav Golda",
            "Dzhyncharadze George", "iAlexander", "Maksim Khabenko");
        for (Certificate certificate : result) {
            final X509Certificate cert = (X509Certificate) certificate;
            final sun.security.x509.X500Name subjectDN = (sun.security.x509.X500Name) cert.getSubjectDN();
            final String commonName = subjectDN.getCommonName();
            assertTrue(names.contains(commonName), commonName + " is not present in expected names set " +
                "for certificates: " + names + ", cert: " + certificate);
        }

    }

    @Test
    public void testReadDebugCertificates() throws IOException {

        // init certificates (from apl-updater/src/main/resources/debug-certs)
        UpdaterUtil.init(true);

        // read certificates ignoring input parameters
        Set<Certificate> result = UpdaterUtil.readCertificates("any-dir",
            "any-prefix", "any-suffix");

        assertNotNull(result);
        assertEquals(1, result.size()); // same cert

        final Certificate certificate = (Certificate) result.toArray()[0];
        final X509Certificate cert = (X509Certificate) certificate;
        final sun.security.x509.X500Name subjectDN = (sun.security.x509.X500Name) cert.getSubjectDN();
        final String commonName = subjectDN.getCommonName();
        assertEquals("Andrii Boiarskyi", commonName, commonName + " does not match expected " +
            "www.firstbridge.io for loaded debug certificate");
    }

    private void assertHasPair(Set<CertificatePair> pairs, String devName, String approver) {
        for (CertificatePair pair : pairs) {
            final String firstName = getNameFromCert(pair.getSecondCertificate());
            if (!firstName.equals(devName)) {
                continue;
            }
            final String secondName = getNameFromCert(pair.getFirstCertificate());
            if (secondName.equals(approver)) {
                return;
            }
        }
        fail(pairs + " does not contain certificate pair for 1-dev: " + devName + " and 2-approver: " + approver);
    }

    private String getNameFromCert(Certificate certificate) {
        final X509Certificate cert = (X509Certificate) certificate;
        final sun.security.x509.X500Name subjectDN = (sun.security.x509.X500Name) cert.getSubjectDN();
        final String commonName;
        try {
            commonName = subjectDN.getCommonName();
        } catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return commonName;
    }

}
