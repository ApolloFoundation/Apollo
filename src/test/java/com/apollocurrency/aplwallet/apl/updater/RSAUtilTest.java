/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import static com.apollocurrency.aplwallet.apl.updater.decryption.RSAUtil.decrypt;
import static com.apollocurrency.aplwallet.apl.updater.decryption.RSAUtil.doubleDecrypt;
import static com.apollocurrency.aplwallet.apl.updater.decryption.RSAUtil.doubleEncrypt;
import static com.apollocurrency.aplwallet.apl.updater.decryption.RSAUtil.encrypt;
import static com.apollocurrency.aplwallet.apl.updater.decryption.RSAUtil.getPrivateKey;
import static com.apollocurrency.aplwallet.apl.updater.decryption.RSAUtil.getPublicKeyFromCertificate;
import static org.slf4j.LoggerFactory.getLogger;

import java.security.PrivateKey;
import java.security.PublicKey;

import com.apollocurrency.aplwallet.apl.updater.decryption.RSADoubleDecryptor;
import com.apollocurrency.aplwallet.apl.updater.decryption.RSAUtil;
import static com.apollocurrency.aplwallet.apl.updater.decryption.RSAUtil.decrypt;
import static com.apollocurrency.aplwallet.apl.updater.decryption.RSAUtil.doubleDecrypt;
import static com.apollocurrency.aplwallet.apl.updater.decryption.RSAUtil.doubleEncrypt;
import static com.apollocurrency.aplwallet.apl.updater.decryption.RSAUtil.encrypt;
import static com.apollocurrency.aplwallet.apl.updater.decryption.RSAUtil.getPrivateKey;
import static com.apollocurrency.aplwallet.apl.updater.decryption.RSAUtil.getPublicKeyFromCertificate;
import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.updater.decryption.RSADoubleDecryptor;
import com.apollocurrency.aplwallet.apl.updater.decryption.RSAUtil;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;


public class RSAUtilTest {
    private static final Logger LOG = getLogger(RSAUtilTest.class);

    @Test
    public void testEncryptAndDecrypt() throws Exception {
        PublicKey pubKey = getPublicKeyFromCertificate("certs/1_1.crt");
        PrivateKey privateKey = getPrivateKey("certs/1_1.key");

        // encrypt the message
        String expectedMessage = "This is a secret message";
        byte[] encrypted = encrypt(privateKey, expectedMessage.getBytes());
        System.out.println("Encrypted message in hex:");
        System.out.println(Convert.toHexString(encrypted));
        // decrypt the message

        byte[] secret = decrypt(pubKey, encrypted);
        String actual = new String(secret);

        System.out.println("Decrypted message:");
        System.out.println(actual);

        Assert.assertEquals(expectedMessage, actual);
    }

    @Test
    public void doubleEncryptAndDecrypt() throws Exception {
        PublicKey pubKey1 = getPublicKeyFromCertificate("certs/1_2.crt");
        PrivateKey privateKey1 = getPrivateKey("certs/1_2.key");

        PublicKey pubKey2 = getPublicKeyFromCertificate("certs/2_2.crt");
        PrivateKey privateKey2 = getPrivateKey("certs/2_2.key");

        String expectedMessage = "This is a secret message!";

        DoubleByteArrayTuple doubleEncryptedMessageBytes = doubleEncrypt(privateKey1, privateKey2, expectedMessage.getBytes());

        byte[] doubleDecryptedMessageBytes = doubleDecrypt(pubKey2, pubKey1, doubleEncryptedMessageBytes);

        String actual = new String(doubleDecryptedMessageBytes);

        System.out.println("Decrypted message:");
        System.out.println(actual);

        Assert.assertEquals(expectedMessage, actual);
    }

    @Test
    public void testDecryptUrl() throws Exception {
        PublicKey pubKey1 = getPublicKeyFromCertificate("certs/1_2.crt");
        PrivateKey privateKey1 = getPrivateKey("certs/1_2.key");

        PublicKey pubKey2 = getPublicKeyFromCertificate("certs/2_2.crt");
        PrivateKey privateKey2 = getPrivateKey("certs/2_2.key");

        String expectedMessage = "http://apollocurrency/ApolloWallet-1.0.8.jar";
        DoubleByteArrayTuple doubleEncryptedBytes = RSAUtil.doubleEncrypt(privateKey1, privateKey2, expectedMessage.getBytes());

        byte[] encryptedBytes = ArrayUtils.addAll(doubleEncryptedBytes.getFirst(),
                doubleEncryptedBytes.getSecond());

        String url = new String(new RSADoubleDecryptor().decrypt(encryptedBytes,
                pubKey2, pubKey1));
        Assert.assertNotNull(url);
        Assert.assertEquals(expectedMessage, url);
    }
}
