/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.apl.crypto;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;

/**
 *
 * @author alukin@gmail.com
 */
public class CryptoTest {
    private static final String TST_IN_DIR="testdata/input/";
    private static final String TST_OUT_DIR="testdata/out/";
            
    private static final String PLAIN_FILE_TEXT = "lorem_ipsum.txt";
    private static final String OUT_FILE_ENCRYPTED = "encrypt_test.bin";
    private static final String OUT_FILE_KEYSEED_S2N = "keyseed_srtring2nonces_test.bin";
    private static final String OUT_FILE_KEYSEED_B2N = "keyseed_bytes2nonces_test.bin";
    private static final String OUT_FILE_KEYSEED_S = "keyseed_string_test.bin";    
    private static final String OUT_FILE_PUBKEY_S = "pubkey_string_test.bin";    
    private static final String OUT_FILE_PRIVKEY_B = "privkey_string_b_test.bin";    
    private static final String OUT_FILE_PRIVKEY_S = "privkey_string_s_test.bin";    
    private static final String OUT_FILE_SIGN_B = "sign_string_b_test.bin";    
    private static final String OUT_FILE_SIGN_S = "sign_string_s_test.bin";    
    private static final String OUT_FILE_SHARED = "shared_key_test.bin";    
    private static final String OUT_FILE_SHARED_NONCE = "shared_key_nonce_test.bin";    
    private static final String OUT_FILE_AES = "aes_encrypt_test.bin";    
    private static final String OUT_FILE_AES_GCM = "aes_gcm_encrypt_test.bin";    
    private static byte[] plain_data;
    private static final byte[] nonce1 = new byte[32]; //(0-31)
    private static final byte[] nonce2 = new byte[32]; //(32-63)
    private static final String secretPhraseA = "Red fox jumps over the Lazy dog";
    private static final String secretPhraseB = "Red dog jumps over the Lazy fox";
    
    private static void writeToFile(ByteBuffer data, String fileName) throws IOException {
        FileChannel out = new FileOutputStream(fileName).getChannel();
        data.rewind();
        out.write(data);
        out.close();
    }

    private static ByteBuffer readFromFile(String fileName) throws IOException {
        
        FileChannel fChan;
        Long fSize;
        ByteBuffer mBuf;
        fChan = new FileInputStream(fileName).getChannel();
        fSize = fChan.size();
        mBuf = ByteBuffer.allocate(fSize.intValue());
        fChan.read(mBuf);
        fChan.close();
        mBuf.rewind();
        return mBuf;
    }    

    public CryptoTest() {
    }
    
    @BeforeAll
    public static void setUpClass() {
        String inFile=TST_IN_DIR + PLAIN_FILE_TEXT;

        try {

            Path currentRelativePath = Paths.get("");
            String s = currentRelativePath.toAbsolutePath().toString();
            System.out.println("===== Current absolute path is: " + s+" ==========");
            ByteBuffer pd = readFromFile(inFile);
            plain_data = pd.array();
            File directory = new File(TST_OUT_DIR);
            if (! directory.exists()){
                directory.mkdirs();
            }
    
            writeToFile(pd, TST_OUT_DIR + PLAIN_FILE_TEXT);
            for (Integer i = 0; i < 32; i++) {
                nonce1[i] = i.byteValue();
                nonce2[i] = new Integer(i + 32).byteValue();
            }
        } catch (IOException ex) {
            fail("Can not read input data file: " + inFile);
        }
    }

    /**
     * Test of getMessageDigest method, of class Crypto.
     */
    @Test
    public void testGetMessageDigest() {
        String[] algorithms = {"SHA-256", "SHA-512"};
        //calcualted by openssl
        String[] digests = {
            "1996b1473fbac5f5aa3e2c81e1fb31a5580eb8463bca2b3121ca94a1cfe4cfea", //SHA-256
            "101e03fa3e90c18825a8286e224f1f680f045d24a8d97b7c813a52fcc7e561cc6eec7ebec83c32c2b949fb0ed0de4473239027f99758fc4d9d28295d0672896b" //sha512

        };
        MessageDigest expResult = null;
        for(int i=0; i<algorithms.length; i++){
            MessageDigest result = Crypto.getMessageDigest(algorithms[i]);
            result.update(plain_data);
            byte[] d = result.digest();
            byte[] ed = Convert.parseHexString(digests[i]);
            assertArrayEquals(ed, d);
        }

    }

 
    /**
     * Test of getKeySeed method, of class Crypto.
     */
    @Test
    public void testGetKeySeed_String_byteArrArr() throws IOException {
          byte[] result = Crypto.getKeySeed(secretPhraseA, nonce1, nonce2);
          byte[] expResult = Convert.parseHexString("b1702f2262274290d1428b04f2e55e5af3af413575c7659ac02ee5633c504c6f");
          assertArrayEquals(expResult, result);
          writeToFile(ByteBuffer.wrap(result), TST_OUT_DIR+OUT_FILE_KEYSEED_S2N);
    }

    /**
     * Test of getKeySeed method, of class Crypto.
     */
    @Test
    public void testGetKeySeed_byteArr_byteArrArr() throws IOException {
          byte[] result = Crypto.getKeySeed(secretPhraseA.getBytes(), nonce1, nonce2);
          byte[] expResult = Convert.parseHexString("b1702f2262274290d1428b04f2e55e5af3af413575c7659ac02ee5633c504c6f");
          assertArrayEquals(expResult, result);
          writeToFile(ByteBuffer.wrap(result), TST_OUT_DIR+OUT_FILE_KEYSEED_B2N);
    }

    /**
     * Test of getPublicKey method, of class Crypto.
     */
    @Test
    public void testGetPublicKey_byteArr() throws IOException {
          byte[] result = Crypto.getKeySeed(secretPhraseA.getBytes());
          byte[] expResult = Convert.parseHexString("b0f12497c84af1ac2603f97d1fb804fc308e241d522fa5d21e900facbb92d6ee");
          assertArrayEquals(expResult, result);
          writeToFile(ByteBuffer.wrap(result), TST_OUT_DIR+OUT_FILE_PRIVKEY_B);
    }

    /**
     * Test of getPublicKey method, of class Crypto.
     */
    @Test
    public void testGetPublicKey_String() throws IOException {
          byte[] expResult = Convert.parseHexString("1b93c9dd30b8fb288463b3fd004c555ceb635c085642ef25d733275fcc33a47b");
          byte[] result = Crypto.getPublicKey(secretPhraseA);
          assertArrayEquals(expResult, result);
          writeToFile(ByteBuffer.wrap(result), TST_OUT_DIR+OUT_FILE_PUBKEY_S);
    }

    /**
     * Test of getPrivateKey method, of class Crypto.
     */
    @Test
    public void testGetPrivateKey_byteArr() throws IOException {
           byte[] keySeed =   Crypto.getKeySeed(secretPhraseA.getBytes());
           byte[] expResult =  Convert.parseHexString("b0f12497c84af1ac2603f97d1fb804fc308e241d522fa5d21e900facbb92d66e");
           byte[] result = Crypto.getPrivateKey(keySeed);
           assertArrayEquals(expResult, result);
           writeToFile(ByteBuffer.wrap(result), TST_OUT_DIR+OUT_FILE_PRIVKEY_B);
    }

    /**
     * Test of getPrivateKey method, of class Crypto.
     */
    @Test
    public void testGetPrivateKey_String() throws IOException {
           byte[] keySeed =   Crypto.getKeySeed(secretPhraseA);
           byte[] expResult =  Convert.parseHexString("b0f12497c84af1ac2603f97d1fb804fc308e241d522fa5d21e900facbb92d66e");
           byte[] result = Crypto.getPrivateKey(keySeed);          
           assertArrayEquals(expResult, result);
           writeToFile(ByteBuffer.wrap(result), TST_OUT_DIR+OUT_FILE_PRIVKEY_S);
    }

    /**
     * Test of curve method, of class Crypto.
     */
    @Test
    public void testCurve() {
//        System.out.println("curve");
//        byte[] Z = null;
//        byte[] k = null;
//        byte[] P = null;
//        Crypto.curve(Z, k, P);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of sign method, of class Crypto.
     */
    @Test
    public void testSign_byteArr_String() throws IOException {
          byte[] message = plain_data;
          byte[] expResult = Convert.parseHexString("f565212c53a668006fbdb12c512e51f7add8118e6573d5c7261e9f58944e5c0b0ae76275210b795915a3017852fe8bca1a3cd2d2b02b32a51e0e03b18e6335f8");
          byte[] result = Crypto.sign(message, secretPhraseA);          
          assertArrayEquals(expResult, result);                     
          writeToFile(ByteBuffer.wrap(result), TST_OUT_DIR+OUT_FILE_SIGN_S);

    }

    /**
     * Test of sign method, of class Crypto.
     */
    @Test
    public void testSign_byteArr_byteArr() throws IOException {
          byte[] message = plain_data;
          byte[] expResult = Convert.parseHexString("f565212c53a668006fbdb12c512e51f7add8118e6573d5c7261e9f58944e5c0b0ae76275210b795915a3017852fe8bca1a3cd2d2b02b32a51e0e03b18e6335f8");
          byte[] result = Crypto.sign(message, Crypto.getKeySeed(secretPhraseA));          
          assertArrayEquals(expResult, result);                     
          writeToFile(ByteBuffer.wrap(result), TST_OUT_DIR+OUT_FILE_SIGN_B);
    }

    /**
     * Test of verify method, of class Crypto.
     */
    @Test
    public void testVerify() throws IOException {
        byte[] signature = Convert.parseHexString("f565212c53a668006fbdb12c512e51f7add8118e6573d5c7261e9f58944e5c0b0ae76275210b795915a3017852fe8bca1a3cd2d2b02b32a51e0e03b18e6335f8");
        byte[] message =plain_data;
        byte[] publicKey = Crypto.getPublicKey(secretPhraseA);
        boolean expResult = true;
        boolean result = Crypto.verify(signature, message, publicKey);
        assertEquals(expResult, result);
    }

    /**
     * Test of getSharedKey method, of class Crypto.
     */
    @Test
    public void testGetSharedKey_byteArr_byteArr() throws IOException {
          byte[] myPrivateKey = Crypto.getPrivateKey(secretPhraseA);
          byte[] theirPublicKey = Crypto.getPublicKey(secretPhraseB);
          byte[] expResult =  Crypto.getSharedKey(myPrivateKey, theirPublicKey);
          byte[] theirPrivateKey = Crypto.getPrivateKey(secretPhraseB);
          byte[] myPublicKey = Crypto.getPublicKey(secretPhraseA);
          byte[] result = Crypto.getSharedKey(theirPrivateKey, myPublicKey);
          assertArrayEquals(expResult, result);
          writeToFile(ByteBuffer.wrap(result), TST_OUT_DIR+OUT_FILE_SHARED);
    }

    /**
     * Test of getSharedKey method, of class Crypto.
     */
    @Test
    public void testGetSharedKey_3args() throws IOException {
          byte[] myPrivateKey = Crypto.getPrivateKey(secretPhraseA);
          byte[] theirPublicKey = Crypto.getPublicKey(secretPhraseB);
          byte[] expResult =  Crypto.getSharedKey(myPrivateKey, theirPublicKey, nonce1);
          byte[] theirPrivateKey = Crypto.getPrivateKey(secretPhraseB);
          byte[] myPublicKey = Crypto.getPublicKey(secretPhraseA);
          byte[] result = Crypto.getSharedKey(theirPrivateKey, myPublicKey, nonce1);
          assertArrayEquals(expResult, result);
          writeToFile(ByteBuffer.wrap(result), TST_OUT_DIR+OUT_FILE_SHARED_NONCE);
    }

    /**
     * Test of aesEncrypt method, of class Crypto.
     */
    @Test
    public void testAesEncrypt() throws IOException {
        byte[] plaintext =plain_data;
        byte[] myPrivateKey = Crypto.getPrivateKey(secretPhraseA);
        byte[] theirPublicKey = Crypto.getPublicKey(secretPhraseB);
        byte[] key =  Crypto.getSharedKey(myPrivateKey, theirPublicKey);        
        byte[] expResult = plaintext;
        byte[] result_enc = Crypto.aesEncrypt(plaintext, key);
        byte[] result = Crypto.aesDecrypt(result_enc, key);
        assertArrayEquals(expResult, result);
        writeToFile(ByteBuffer.wrap(result_enc), TST_OUT_DIR+OUT_FILE_AES);
    }

    /**
     * Test of aesGCMEncrypt method, of class Crypto.
     */
    @Test
    public void testAesGCMEncrypt() throws IOException {
        byte[] plaintext =plain_data;
        byte[] myPrivateKey = Crypto.getPrivateKey(secretPhraseA);
        byte[] theirPublicKey = Crypto.getPublicKey(secretPhraseB);
        byte[] key =  Crypto.getSharedKey(myPrivateKey, theirPublicKey);        
        byte[] expResult = plaintext;
        byte[] result_enc = Crypto.aesGCMEncrypt(plaintext, key);
        byte[] result = Crypto.aesGCMDecrypt(result_enc, key);
        assertArrayEquals(expResult, result);
        writeToFile(ByteBuffer.wrap(result_enc), TST_OUT_DIR+OUT_FILE_AES_GCM);
    }

    /**
     * Test of rsEncode method, of class Crypto.
     */
    @Test
    public void testRsEncode() {

        long id = 0L;
        String expResult = "2222-2222-2222-22222";
        String result = Crypto.rsEncode(id);
        assertEquals(expResult, result);
    }

    /**
     * Test of rsDecode method, of class Crypto.
     */
    @Test
    public void testRsDecode() {
        String rsString = "2222-2222-2222-22222";
        long expResult = 0L;
        long result = Crypto.rsDecode(rsString);
        assertEquals(expResult, result);
    }

    /**
     * Test of isCanonicalPublicKey method, of class Crypto.
     */
    @Test
    public void testIsCanonicalPublicKey() {
        byte[] publicKey = Crypto.getPublicKey(secretPhraseA);
        boolean expResult = true;
        boolean result = Crypto.isCanonicalPublicKey(publicKey);
        assertEquals(expResult, result);
    }

    /**
     * Test of isCanonicalSignature method, of class Crypto.
     */
    @Test
    public void testIsCanonicalSignature() {
        byte[] message = plain_data;
        byte[] signature = Crypto.sign(message, Crypto.getKeySeed(secretPhraseA));       
        boolean expResult = true;
        boolean result = Crypto.isCanonicalSignature(signature);
        assertEquals(expResult, result);
    }


    /**
     * Test of elGamalDecrypt method, of class Crypto.
     */
    @Test
    public void testElGamalDecrypt() {
//TODO:  test by encrypt and then decript plain_data

    }
    
}
