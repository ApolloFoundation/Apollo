package com.apollocurrency.apl.id.cert;

import io.firstbridge.cryptolib.CryptoConfig;
import io.firstbridge.cryptolib.CryptoParams;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author alukin@gmail.com
 */
public class CertificateTest {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(CertificateTest.class);
    static CertHelper acert;

    public CertificateTest() {
    }

    @BeforeAll
    public static void setUpClass() {
        try (InputStream is = CertificateTest.class.getClassLoader().getResourceAsStream("test_cert.pem")) {
            acert = CertHelper.loadPEMFromStream(is);
        } catch (IOException ex) {
            log.error("Can not load test certificate ", ex);
        } catch (CertException ex) {
            log.error("can not parse test certificate", ex);
        }
    }

    /**
     * Test of getAuthorityId method, of class ApolloCertificate.
     */
    @Test
    public void testGetAuthorityId() {
        AuthorityID result = acert.getAuthorityId();
        assertEquals(5139, result.getActorTypeAsInt());
    }

    /**
     * Test of getCN method, of class ApolloCertificate.
     */
    @Test
    public void testGetCN() {
        String result = acert.getCN();
        assertEquals("al.cn.ua", result);
    }

    /**
     * Test of getOrganization method, of class ApolloCertificate.
     */
    @Test
    public void testGetOrganization() {
        String expResult = "FirstBridge";
        String result = acert.getOrganization();
        assertEquals(expResult, result);
    }

    /**
     * Test of getOrganizationUnit method, of class ApolloCertificate.
     */
    @Test
    public void testGetOrganizationUnit() {
        String expResult = "FB-cn";
        String result = acert.getOrganizationUnit();
        assertEquals(expResult, result);
    }

    /**
     * Test of getCountry method, of class ApolloCertificate.
     */
    @Test
    public void testGetCountry() {
        String expResult = "UA";
        String result = acert.getCountry();
        assertEquals(expResult, result);
    }

    /**
     * Test of getCity method, of class ApolloCertificate.
     */
    @Test
    public void testGetCity() {
        String expResult = "Chernigiv";
        String result = acert.getCity();
        assertEquals(expResult, result);
    }

    /**
     * Test of getCertificatePurpose method, of class ApolloCertificate.
     */
    @Test
    public void testGetCertificatePurpose() {
        String expResult = "Node";
        String result = acert.getCertificatePurpose();
        assertEquals(expResult, result);
    }

    /**
     * Test of getIPAddresses method, of class ApolloCertificate.
     */
    @Test
    public void testGetIPAddresses() {
        List<String> expResult = null;
        List<String> result = acert.getIPAddresses();
        assertEquals(expResult, result);
    }

    /**
     * Test of getDNSNames method, of class ApolloCertificate.
     */
    @Test
    public void testGetDNSNames() {
        List<String> expResult = null;
        List<String> result = acert.getDNSNames();
        assertEquals(expResult, result);
    }

    /**
     * Test of getStateOrProvince method, of class ApolloCertificate.
     */
    @Test
    public void testGetStateOrProvince() {
        String expResult = null;
        String result = acert.getStateOrProvince();
        assertEquals(expResult, result);
    }

    /**
     * Test of getEmail method, of class ApolloCertificate.
     */
    @Test
    public void testGetEmail() {
        String expResult = "alukin@gmail.com";
        String result = acert.getEmail();
        assertEquals(expResult, result);
    }

    /**
     * Test of getCertPEM method, of class ApolloCertificate.
     */
    @Test
    public void testGetPEM() {
        String result = acert.getCertPEM();
        assertEquals(true, result.startsWith("-----BEGIN CERTIFICATE----"));
    }

    /**
     * Test of isValid method, of class ApolloCertificate.
     */
    @Test
    public void testIsValid() {
        Date date = null;
        boolean expResult = false;
        boolean result = acert.isValid(date);
        assertEquals(expResult, result);
    }

    /**
     * Test of getSerial method, of class ApolloCertificate.
     */
    @Test
    public void testGetSerial() {
        BigInteger result = acert.getSerial();
        BigInteger expResult = BigInteger.valueOf(1582313240538L);
        assertEquals(expResult, result);
    }

    @Test
    public void testGetIssuerAttributes() {
        CertAttributes cert_attr = acert.getIssuerAttrinutes();
        assertEquals("al.cn.ua", cert_attr.getCn());
        assertEquals("FirstBridge", cert_attr.getO());
    }

}
