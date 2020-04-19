/*
 *  Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.crypto;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author alukin@gmail.com
 */
public class EncryptedDataTest extends TestsCommons{

    private static final String OUT_FILE_ENCRYPTED = "encrypted_data_test.bin";

    
    public EncryptedDataTest() {
    }

    
   
    /**
     * Test of encrypt method, of class EncryptedData.
     * @throws java.io.IOException
     */
    @Test
    public void testEncrypt() throws IOException {
        byte[] plaintext = plain_data;
        byte[] keySeed = Crypto.getKeySeed(secretPhraseA);
        byte[] theirPublicKey = Crypto.getPublicKey(secretPhraseB);
        EncryptedData result_enc = EncryptedData.encrypt(plaintext, keySeed, theirPublicKey);
        byte[] plain_res = result_enc.decrypt(keySeed, theirPublicKey);
        assertArrayEquals(plaintext, plain_res);
        writeToFile(ByteBuffer.wrap(result_enc.getBytes()), TST_OUT_DIR+OUT_FILE_ENCRYPTED);
    }

    /**
     * Test of readEncryptedData method, of class EncryptedData.
     */
    @Test
    public void testReadEncryptedData_3args() throws Exception {
        byte[] plaintext = plain_data;
        byte[] keySeed = Crypto.getKeySeed(secretPhraseA);
        byte[] theirPublicKey = Crypto.getPublicKey(secretPhraseB);
        EncryptedData result_enc = EncryptedData.encrypt(plaintext, keySeed, theirPublicKey);
        EncryptedData result = EncryptedData.readEncryptedData(
                ByteBuffer.wrap(result_enc.getBytes()), 
                result_enc.getSize()-result_enc.getNonce().length, 
                result_enc.getSize()
             );
        byte[] plain_res = result_enc.decrypt(keySeed, theirPublicKey);
        assertArrayEquals(plaintext, plain_res);       
    }

    /**
     * Test of readEncryptedData method, of class EncryptedData.
     */
    @Test
    public void testReadEncryptedData_byteArr() {
        byte[] plaintext = plain_data;
        byte[] keySeed = Crypto.getKeySeed(secretPhraseA);
        byte[] theirPublicKey = Crypto.getPublicKey(secretPhraseB);
        EncryptedData result_enc = EncryptedData.encrypt(plaintext, keySeed, theirPublicKey);
        EncryptedData result = EncryptedData.readEncryptedData(
                result_enc.getBytes()
             );
        byte[] plain_res = result_enc.decrypt(keySeed, theirPublicKey);
        assertArrayEquals(plaintext, plain_res);
    }
    
}
