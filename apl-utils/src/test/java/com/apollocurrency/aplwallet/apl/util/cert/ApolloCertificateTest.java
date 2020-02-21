
package com.apollocurrency.aplwallet.apl.util.cert;

import io.firstbridge.cryptolib.FBCryptoParams;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;
import org.slf4j.LoggerFactory;

/**
 *
 * @author alukin@gmail.com
 */
@Disabled
//TODO: KeyFactory stopped read PEM cers, it is strange
public class ApolloCertificateTest {
    private static final FBCryptoParams params = FBCryptoParams.createDefault();
    static ApolloCertificate acert;
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ApolloCertificateTest.class);
    public ApolloCertificateTest() {
    }
    
    @BeforeAll
    public static void setUpClass() {
        try {
            acert = ApolloCertificate.loadPEMFromStream(ApolloCertificateTest.class.getClassLoader().getResourceAsStream("test_cert.pem"));
            acert.parseAttributes();
        } catch (IOException ex) {
            log.error("Can not load test certificate ", ex);
        } catch (ApolloCertificateException ex) {
            log.error("can not parse test certificate", ex);
        }
    }


    /**
     * Test of getAuthorityId method, of class ApolloCertificate.
     */
    @Test
    public void testGetAuthorityId() {
        System.out.println("getAuthorityId");
        AuthorityID result = acert.getAuthorityId();
        assertEquals(12, result.getActorType());
    }

    /**
     * Test of getCN method, of class ApolloCertificate.
     */
    @Test
    public void testGetCN() {
        System.out.println("getCN");
        String result = acert.getCN();
        assertEquals("al.cn.ua", result);
    }

    /**
     * Test of getOrganization method, of class ApolloCertificate.
     */
    @Test
    public void testGetOrganization() {
        System.out.println("getOrganization");
        ApolloCertificate instance = null;
        String expResult = "FirstBridge";
        String result = acert.getOrganization();
        assertEquals(expResult, result);
    }

    /**
     * Test of getOrganizationUnit method, of class ApolloCertificate.
     */
    @Test
    public void testGetOrganizationUnit() {
        String expResult = "FB-CN";
        String result = acert.getOrganizationUnit();
        assertEquals(expResult, result);
    }

    /**
     * Test of getCountry method, of class ApolloCertificate.
     */
    @Test
    public void testGetCountry() {
        System.out.println("getCountry");
        ApolloCertificate instance = null;
        String expResult = "";
        String result = instance.getCountry();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getCity method, of class ApolloCertificate.
     */
    @Test
    public void testGetCity() {
        System.out.println("getCity");
        ApolloCertificate instance = null;
        String expResult = "";
        String result = acert.getCity();
        assertEquals(expResult, result);
    }

    /**
     * Test of getCertificatePurpose method, of class ApolloCertificate.
     */
    @Test
    public void testGetCertificatePurpose() {
        System.out.println("getCertificatePurpose");
        String expResult = "";
        String result = acert.getCertificatePurpose();
        assertEquals(expResult, result);
    }

    /**
     * Test of getIPAddresses method, of class ApolloCertificate.
     */
    @Test
    public void testGetIPAddresses() {
        System.out.println("getIPAddresses");
        List<String> expResult = new ArrayList<>();
        List<String> result = acert.getIPAddresses();
        assertEquals(expResult, result);
    }

    /**
     * Test of getDNSNames method, of class ApolloCertificate.
     */
    @Test
    public void testGetDNSNames() {
        System.out.println("getDNSNames");
        ApolloCertificate instance = null;
        List<String> expResult = new ArrayList<>();
        List<String> result = instance.getDNSNames();
        assertEquals(expResult, result);
    }

    /**
     * Test of getStateOrProvince method, of class ApolloCertificate.
     */
    @Test
    public void testGetStateOrProvince() {
        System.out.println("getStateOrProvince");
        String expResult = "";
        String result = acert.getStateOrProvince();
        assertEquals(expResult, result);
    }

    /**
     * Test of getEmail method, of class ApolloCertificate.
     */
    @Test
    public void testGetEmail() {
        System.out.println("getEmail");
        String expResult = "alukin@gmail.com";
        String result = acert.getEmail();
        assertEquals(expResult, result);
    }

    /**
     * Test of fromList method, of class ApolloCertificate.
     */
    @Test
    public void testFromList() {
        System.out.println("fromList");
    }

    /**
     * Test of getPEM method, of class ApolloCertificate.
     */
    @Test
    public void testGetPEM() {
        System.out.println("getPEM");
        String expResult = "";
        String result = acert.getPEM();
        assertEquals(expResult, result);
    }

    /**
     * Test of isValid method, of class ApolloCertificate.
     */
    @Test
    public void testIsValid() {
        System.out.println("isValid");
        Date date = null;
        ApolloCertificate instance = null;
        boolean expResult = false;
        boolean result = instance.isValid(date);
        assertEquals(expResult, result);
    }

    /**
     * Test of getSerial method, of class ApolloCertificate.
     */
    @Test
    public void testGetSerial() {
        System.out.println("getSerial");
        BigInteger expResult = null;
        BigInteger result = acert.getSerial();
        assertEquals(expResult, result);
    }
    
}
