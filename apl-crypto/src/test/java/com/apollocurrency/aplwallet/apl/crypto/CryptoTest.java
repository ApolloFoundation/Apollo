/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.apl.crypto;

import io.firstbridge.cryptolib.dataformat.FBElGamalKeyPair;
import java.security.MessageDigest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author alukin@gmail.com
 */
public class CryptoTest {
    
    public CryptoTest() {
    }
    


    /**
     * Test of getMessageDigest method, of class Crypto.
     */
    @Test
    public void testGetMessageDigest() {
        System.out.println("getMessageDigest");
        String algorithm = "";
        MessageDigest expResult = null;
        MessageDigest result = Crypto.getMessageDigest(algorithm);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of sha256 method, of class Crypto.
     */
    @Test
    public void testSha256() {
        System.out.println("sha256");
        MessageDigest expResult = null;
        MessageDigest result = Crypto.sha256();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of sha512 method, of class Crypto.
     */
    @Test
    public void testSha512() {
        System.out.println("sha512");
        MessageDigest expResult = null;
        MessageDigest result = Crypto.sha512();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of ripemd160 method, of class Crypto.
     */
    @Test
    public void testRipemd160() {
        System.out.println("ripemd160");
        MessageDigest expResult = null;
        MessageDigest result = Crypto.ripemd160();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of sha3 method, of class Crypto.
     */
    @Test
    public void testSha3() {
        System.out.println("sha3");
        MessageDigest expResult = null;
        MessageDigest result = Crypto.sha3();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getKeySeed method, of class Crypto.
     */
    @Test
    public void testGetKeySeed_String_byteArrArr() {
        System.out.println("getKeySeed");
        String secretPhrase = "";
        byte[][] nonces = null;
        byte[] expResult = null;
        byte[] result = Crypto.getKeySeed(secretPhrase, nonces);
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getKeySeed method, of class Crypto.
     */
    @Test
    public void testGetKeySeed_byteArr_byteArrArr() {
        System.out.println("getKeySeed");
        byte[] secretBytes = null;
        byte[][] nonces = null;
        byte[] expResult = null;
        byte[] result = Crypto.getKeySeed(secretBytes, nonces);
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getPublicKey method, of class Crypto.
     */
    @Test
    public void testGetPublicKey_byteArr() {
        System.out.println("getPublicKey");
        byte[] keySeed = null;
        byte[] expResult = null;
        byte[] result = Crypto.getPublicKey(keySeed);
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getPublicKey method, of class Crypto.
     */
    @Test
    public void testGetPublicKey_String() {
        System.out.println("getPublicKey");
        String secretPhrase = "";
        byte[] expResult = null;
        byte[] result = Crypto.getPublicKey(secretPhrase);
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getPrivateKey method, of class Crypto.
     */
    @Test
    public void testGetPrivateKey_byteArr() {
        System.out.println("getPrivateKey");
        byte[] keySeed = null;
        byte[] expResult = null;
        byte[] result = Crypto.getPrivateKey(keySeed);
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getPrivateKey method, of class Crypto.
     */
    @Test
    public void testGetPrivateKey_String() {
        System.out.println("getPrivateKey");
        String secretPhrase = "";
        byte[] expResult = null;
        byte[] result = Crypto.getPrivateKey(secretPhrase);
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of curve method, of class Crypto.
     */
    @Test
    public void testCurve() {
        System.out.println("curve");
        byte[] Z = null;
        byte[] k = null;
        byte[] P = null;
        Crypto.curve(Z, k, P);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of sign method, of class Crypto.
     */
    @Test
    public void testSign_byteArr_String() {
        System.out.println("sign");
        byte[] message = null;
        String secretPhrase = "";
        byte[] expResult = null;
        byte[] result = Crypto.sign(message, secretPhrase);
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of sign method, of class Crypto.
     */
    @Test
    public void testSign_byteArr_byteArr() {
        System.out.println("sign");
        byte[] message = null;
        byte[] keySeed = null;
        byte[] expResult = null;
        byte[] result = Crypto.sign(message, keySeed);
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of verify method, of class Crypto.
     */
    @Test
    public void testVerify() {
        System.out.println("verify");
        byte[] signature = null;
        byte[] message = null;
        byte[] publicKey = null;
        boolean expResult = false;
        boolean result = Crypto.verify(signature, message, publicKey);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getSharedKey method, of class Crypto.
     */
    @Test
    public void testGetSharedKey_byteArr_byteArr() {
        System.out.println("getSharedKey");
        byte[] myPrivateKey = null;
        byte[] theirPublicKey = null;
        byte[] expResult = null;
        byte[] result = Crypto.getSharedKey(myPrivateKey, theirPublicKey);
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getSharedKey method, of class Crypto.
     */
    @Test
    public void testGetSharedKey_3args() {
        System.out.println("getSharedKey");
        byte[] myPrivateKey = null;
        byte[] theirPublicKey = null;
        byte[] nonce = null;
        byte[] expResult = null;
        byte[] result = Crypto.getSharedKey(myPrivateKey, theirPublicKey, nonce);
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of aesEncrypt method, of class Crypto.
     */
    @Test
    public void testAesEncrypt() {
        System.out.println("aesEncrypt");
        byte[] plaintext = null;
        byte[] key = null;
        byte[] expResult = null;
        byte[] result = Crypto.aesEncrypt(plaintext, key);
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of aesGCMEncrypt method, of class Crypto.
     */
    @Test
    public void testAesGCMEncrypt() {
        System.out.println("aesGCMEncrypt");
        byte[] plaintext = null;
        byte[] key = null;
        byte[] expResult = null;
        byte[] result = Crypto.aesGCMEncrypt(plaintext, key);
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of aesDecrypt method, of class Crypto.
     */
    @Test
    public void testAesDecrypt() {
        System.out.println("aesDecrypt");
        byte[] ivCiphertext = null;
        byte[] key = null;
        byte[] expResult = null;
        byte[] result = Crypto.aesDecrypt(ivCiphertext, key);
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of aesGCMDecrypt method, of class Crypto.
     */
    @Test
    public void testAesGCMDecrypt() {
        System.out.println("aesGCMDecrypt");
        byte[] ivCiphertext = null;
        byte[] key = null;
        byte[] expResult = null;
        byte[] result = Crypto.aesGCMDecrypt(ivCiphertext, key);
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of rsEncode method, of class Crypto.
     */
    @Test
    public void testRsEncode() {
        System.out.println("rsEncode");
        long id = 0L;
        String expResult = "";
        String result = Crypto.rsEncode(id);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of rsDecode method, of class Crypto.
     */
    @Test
    public void testRsDecode() {
        System.out.println("rsDecode");
        String rsString = "";
        long expResult = 0L;
        long result = Crypto.rsDecode(rsString);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isCanonicalPublicKey method, of class Crypto.
     */
    @Test
    public void testIsCanonicalPublicKey() {
        System.out.println("isCanonicalPublicKey");
        byte[] publicKey = null;
        boolean expResult = false;
        boolean result = Crypto.isCanonicalPublicKey(publicKey);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isCanonicalSignature method, of class Crypto.
     */
    @Test
    public void testIsCanonicalSignature() {
        System.out.println("isCanonicalSignature");
        byte[] signature = null;
        boolean expResult = false;
        boolean result = Crypto.isCanonicalSignature(signature);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getElGamalKeyPair method, of class Crypto.
     */
    @Test
    public void testGetElGamalKeyPair() {
        System.out.println("getElGamalKeyPair");
        FBElGamalKeyPair expResult = null;
        FBElGamalKeyPair result = Crypto.getElGamalKeyPair();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of elGamalDecrypt method, of class Crypto.
     */
    @Test
    public void testElGamalDecrypt() {
        System.out.println("elGamalDecrypt");
        String cryptogramm = "";
        FBElGamalKeyPair keyPair = null;
        String expResult = "";
        String result = Crypto.elGamalDecrypt(cryptogramm, keyPair);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
