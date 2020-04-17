/*
 *  Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.crypto;

import java.io.IOException;
import java.nio.ByteBuffer;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author alukin@gmail.com
 */
public class AnonymouslyEncryptedDataTest extends TestsCommons {

    private static final String OUT_FILE_ENCRYPTED = "anon_encrypted_data_test.bin";

  

    /**
     * Test of encrypt method, of class AnonymouslyEncryptedData.
     * @throws java.io.IOException
     */
    @Test
    public void testEncrypt() throws IOException {
        byte[] plaintext = plain_data;
        byte[] theirPublicKey = Crypto.getPublicKey(secretPhraseB);
        AnonymouslyEncryptedData result_enc = AnonymouslyEncryptedData.encrypt(plaintext, secretPhraseA.getBytes(), theirPublicKey, nonce1);        
        
        byte[] plain_res = result_enc.decrypt(secretPhraseB.getBytes());
        assertArrayEquals(plaintext, plain_res);
        writeToFile(ByteBuffer.wrap(result_enc.getBytes()), TST_OUT_DIR+OUT_FILE_ENCRYPTED);        

    }

    /**
     * Test of readEncryptedData method, of class AnonymouslyEncryptedData.
     */
    @Test
    public void testReadEncryptedData_3args() throws Exception {
        byte[] plaintext = plain_data;
        byte[] theirPublicKey = Crypto.getPublicKey(secretPhraseB);
        AnonymouslyEncryptedData result_enc = AnonymouslyEncryptedData.encrypt(plaintext, secretPhraseA.getBytes(), theirPublicKey, nonce1);        
        
        AnonymouslyEncryptedData result = AnonymouslyEncryptedData.readEncryptedData(
                ByteBuffer.wrap(result_enc.getBytes()),
                result_enc.getSize()-result_enc.getPublicKey().length,
                result_enc.getSize()
             );
        byte[] plain_res = result_enc.decrypt(secretPhraseB.getBytes());
        assertArrayEquals(plaintext, plain_res);
    }

    /**
     * Test of readEncryptedData method, of class AnonymouslyEncryptedData.
     */
    @Test
    public void testReadEncryptedData_byteArr() {
        byte[] plaintext = plain_data;
        byte[] theirPublicKey = Crypto.getPublicKey(secretPhraseB);
        AnonymouslyEncryptedData result_enc = AnonymouslyEncryptedData.encrypt(plaintext, secretPhraseA.getBytes(), theirPublicKey, nonce1);        
        
        AnonymouslyEncryptedData result = AnonymouslyEncryptedData.readEncryptedData(
                result_enc.getBytes()
             );
        byte[] plain_res = result_enc.decrypt(secretPhraseB.getBytes());
        assertArrayEquals(plaintext, plain_res);
    }

}
