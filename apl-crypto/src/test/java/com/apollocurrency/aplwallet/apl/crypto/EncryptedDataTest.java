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
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author alukin@gmail.com
 */
public class EncryptedDataTest {
    private static final String PLAIN_FILE_TEXT = "../../testdata/input/lorem_ipsum.txt";
    private static final String OUT_FILE_ENCRYPTED = "../../testdata/encrypted_data_test.bin";

    public EncryptedDataTest() {
    }
    
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
    
    /**
     * Test of encrypt method, of class EncryptedData.
     */
    @Test
    public void testEncrypt() throws IOException {
//        System.out.println("encrypt");
//        ByteBuffer plain = readFromFile(PLAIN_FILE_TEXT);  
//        byte[] plaintext = null;
//        byte[] keySeed = null;
//        byte[] theirPublicKey = null;
//        EncryptedData expResult = null;
//        EncryptedData result = EncryptedData.encrypt(plaintext, keySeed, theirPublicKey);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of readEncryptedData method, of class EncryptedData.
     */
    @Test
    public void testReadEncryptedData_3args() throws Exception {
//        System.out.println("readEncryptedData");
//        ByteBuffer buffer = null;
//        int length = 0;
//        int maxLength = 0;
//        EncryptedData expResult = null;
//        EncryptedData result = EncryptedData.readEncryptedData(buffer, length, maxLength);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of readEncryptedData method, of class EncryptedData.
     */
    @Test
    public void testReadEncryptedData_byteArr() {
//        System.out.println("readEncryptedData");
//        byte[] bytes = null;
//        EncryptedData expResult = null;
//        EncryptedData result = EncryptedData.readEncryptedData(bytes);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of getEncryptedDataLength method, of class EncryptedData.
     */
    @Test
    public void testGetEncryptedDataLength() {
//        System.out.println("getEncryptedDataLength");
//        byte[] plaintext = null;
//        int expResult = 0;
//        int result = EncryptedData.getEncryptedDataLength(plaintext);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of getEncryptedSize method, of class EncryptedData.
     */
    @Test
    public void testGetEncryptedSize() {
//        System.out.println("getEncryptedSize");
//        byte[] plaintext = null;
//        int expResult = 0;
//        int result = EncryptedData.getEncryptedSize(plaintext);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of decrypt method, of class EncryptedData.
     */
    @Test
    public void testDecrypt() {
//        System.out.println("decrypt");
//        byte[] keySeed = null;
//        byte[] theirPublicKey = null;
//        EncryptedData instance = null;
//        byte[] expResult = null;
//        byte[] result = instance.decrypt(keySeed, theirPublicKey);
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of getData method, of class EncryptedData.
     */
    @Test
    public void testGetData() {
//        System.out.println("getData");
//        EncryptedData instance = null;
//        byte[] expResult = null;
//        byte[] result = instance.getData();
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of getNonce method, of class EncryptedData.
     */
    @Test
    public void testGetNonce() {
//        System.out.println("getNonce");
//        EncryptedData instance = null;
//        byte[] expResult = null;
//        byte[] result = instance.getNonce();
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of getSize method, of class EncryptedData.
     */
    @Test
    public void testGetSize() {
//        System.out.println("getSize");
//        EncryptedData instance = null;
//        int expResult = 0;
//        int result = instance.getSize();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of getBytes method, of class EncryptedData.
     */
    @Test
    public void testGetBytes() {
//        System.out.println("getBytes");
//        EncryptedData instance = null;
//        byte[] expResult = null;
//        byte[] result = instance.getBytes();
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of equals method, of class EncryptedData.
     */
    @Test
    public void testEquals() {
//        System.out.println("equals");
//        Object o = null;
//        EncryptedData instance = null;
//        boolean expResult = false;
//        boolean result = instance.equals(o);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of hashCode method, of class EncryptedData.
     */
    @Test
    public void testHashCode() {
//        System.out.println("hashCode");
//        EncryptedData instance = null;
//        int expResult = 0;
//        int result = instance.hashCode();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of toString method, of class EncryptedData.
     */
    @Test
    public void testToString() {
//        System.out.println("toString");
//        EncryptedData instance = null;
//        String expResult = "";
//        String result = instance.toString();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }
    
}
