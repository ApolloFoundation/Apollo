
package com.apollocurrency.aplwallet.apl.crypto.fbc;

import io.firstbridge.cryptolib.FBCryptoFactory;
import io.firstbridge.cryptolib.KeyReader;
import io.firstbridge.cryptolib.impl.KeyReaderImpl;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author alukin@gmail.com
 */
public class CryptoMetaFacrotyTest {
    
    public CryptoMetaFacrotyTest() {
    }
    
    @BeforeAll
    public static void setUpClass() {
    }
    
    @AfterAll
    public static void tearDownClass() {
    }
    
    @BeforeEach
    public void setUp() {
    }
    
    @AfterEach
    public void tearDown() {
    }

    /**
     * Test of createFacrory method, of class CryptoMetaFacroty.
     */
    @Test
    public void testCreateFacrory_PublicKey() {
        System.out.println("createFacrory");
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("cert/1_1.crt");
        KeyReader kr = new KeyReaderImpl();        
        X509Certificate cert = kr.readX509CertPEMorDER(is);       
        FBCryptoFactory res =CryptoMetaFacroty.createFacrory(cert.getPublicKey());
    }
    
}
