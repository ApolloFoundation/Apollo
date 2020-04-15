/*
 *  Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 * @author alukin@gmail.com
 */
public class AnonymouslyEncryptedDataTest {
     private static final String TST_IN_DIR="testdata/input/";
    private static final String TST_OUT_DIR="testdata/out/";
    
    private static final String PLAIN_FILE_TEXT = "lorem_ipsum.txt";
    private static final String OUT_FILE_ENCRYPTED = "anon_encrypted_data_test.bin";
    private static byte[] plain_data;
    private static final byte[] nonce1 = new byte[32]; //(0-31)
    private static final byte[] nonce2 = new byte[32]; //(32-63)
    private static final String secretPhraseA = "Red fox jumps over the Lazy dog";
    private static final String secretPhraseB = "Red dog jumps over the Lazy fox";
    
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
      
    private static void writeToFile(ByteBuffer data, String fileName) throws IOException {
        FileChannel out = new FileOutputStream(fileName).getChannel();
        data.rewind();
        out.write(data);
        out.close();
    }
    
    @BeforeAll
    public static void setUpClass() {
        String inFile=TST_IN_DIR + PLAIN_FILE_TEXT;

        try {
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
     * Test of encrypt method, of class AnonymouslyEncryptedData.
     * @throws java.io.IOException
     */
    @Test
    public void testEncrypt() throws IOException {
        byte[] plaintext = plain_data;
        byte[] theirPublicKey = Crypto.getPublicKey(secretPhraseB);
        AnonymouslyEncryptedData result_enc = AnonymouslyEncryptedData.encrypt(plaintext, secretPhraseA.getBytes(), theirPublicKey, nonce1);        

       // byte[] plain_res = result_enc.decrypt(secretPhraseA.getBytes());
       // assertArrayEquals(plaintext, plain_res);
        writeToFile(ByteBuffer.wrap(result_enc.getBytes()), TST_OUT_DIR+OUT_FILE_ENCRYPTED);        

    }

    /**
     * Test of readEncryptedData method, of class AnonymouslyEncryptedData.
     */
    @Test
    public void testReadEncryptedData_3args() throws Exception {

    }

    /**
     * Test of readEncryptedData method, of class AnonymouslyEncryptedData.
     */
    @Test
    public void testReadEncryptedData_byteArr() {

    }

}
