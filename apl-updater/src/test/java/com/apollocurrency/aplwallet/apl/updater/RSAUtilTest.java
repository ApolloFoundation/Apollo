/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.updater.decryption.RSADoubleDecryptor;
import com.apollocurrency.aplwallet.apl.updater.decryption.RSAUtil;
import com.apollocurrency.aplwallet.apl.util.DoubleByteArrayTuple;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static com.apollocurrency.aplwallet.apl.updater.decryption.RSAUtil.decrypt;
import static com.apollocurrency.aplwallet.apl.updater.decryption.RSAUtil.doubleDecrypt;
import static com.apollocurrency.aplwallet.apl.updater.decryption.RSAUtil.doubleEncrypt;
import static com.apollocurrency.aplwallet.apl.updater.decryption.RSAUtil.encrypt;
import static com.apollocurrency.aplwallet.apl.updater.decryption.RSAUtil.getPrivateKey;
import static com.apollocurrency.aplwallet.apl.updater.decryption.RSAUtil.getPublicKeyFromCertificate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.slf4j.LoggerFactory.getLogger;


public class RSAUtilTest {
    private static final Logger log = getLogger(RSAUtilTest.class);

    @Test
    public void testEncryptAndDecrypt() throws Exception {
        PublicKey pubKey = getPublicKeyFromCertificate("test-certs/1_1.crt");
        PrivateKey privateKey = getPrivateKey("test-certs/1_1.key");

        // encrypt the message
        String expectedMessage = "This is a secret message";
        byte[] encrypted = encrypt(privateKey, expectedMessage.getBytes());
        log.debug("Encrypted message in hex: {}", Convert.toHexString(encrypted));
        // decrypt the message

        byte[] secret = decrypt(pubKey, encrypted);
        String actual = new String(secret);

        log.debug("Decrypted message: {}", actual);
        assertEquals(expectedMessage, actual);
    }

    @Test
    public void doubleEncryptAndDecrypt() throws Exception {
        PublicKey pubKey1 = getPublicKeyFromCertificate("test-certs/1_2.crt");
        PrivateKey privateKey1 = getPrivateKey("test-certs/1_2.key");

        PublicKey pubKey2 = getPublicKeyFromCertificate("test-certs/2_2.crt");
        PrivateKey privateKey2 = getPrivateKey("test-certs/2_2.key");

        String expectedMessage = "This is a secret message!";

        DoubleByteArrayTuple doubleEncryptedMessageBytes = doubleEncrypt(privateKey1, privateKey2, expectedMessage.getBytes());

        byte[] doubleDecryptedMessageBytes = doubleDecrypt(pubKey2, pubKey1, doubleEncryptedMessageBytes);

        String actual = new String(doubleDecryptedMessageBytes);

        log.debug("Decrypted message: {}", actual);
        assertEquals(expectedMessage, actual);
    }

    @Test
    public void testDecryptUrl() throws Exception {
        PublicKey pubKey1 = getPublicKeyFromCertificate("test-certs/1_2.crt");
        PrivateKey privateKey1 = getPrivateKey("test-certs/1_2.key");

        PublicKey pubKey2 = getPublicKeyFromCertificate("test-certs/2_2.crt");
        PrivateKey privateKey2 = getPrivateKey("test-certs/2_2.key");

        String expectedMessage = "http://apollocurrency/ApolloWallet-1.0.8.jar";
        DoubleByteArrayTuple doubleEncryptedBytes = RSAUtil.doubleEncrypt(privateKey1, privateKey2, expectedMessage.getBytes());

        byte[] encryptedBytes = ArrayUtils.addAll(doubleEncryptedBytes.getFirst(),
            doubleEncryptedBytes.getSecond());

        String url = new String(new RSADoubleDecryptor().decrypt(encryptedBytes,
            pubKey2, pubKey1));
        assertNotNull(url);
        assertEquals(expectedMessage, url);
    }

    @Test
    void testDecryptUrlEx() throws Exception {
        PublicKey pubKey1 = getPublicKeyFromCertificate("test-certs/1_2.crt");
        PrivateKey privateKey1 = getPrivateKey("test-certs/1_2.key");

        PublicKey pubKey2 = getPublicKeyFromCertificate("test-certs/2_2.crt");
        PrivateKey privateKey2 = getPrivateKey("test-certs/2_2.key");
        Set<CertificatePair> pairs = new HashSet<>();
        String expectedMessage = "http://apollocurrency/ApolloWallet-1.0.8.jar";
        DoubleByteArrayTuple doubleEncryptedBytes = RSAUtil.doubleEncrypt(privateKey1, privateKey2, expectedMessage.getBytes());
        Certificate c1 = UpdaterUtil.readCertificate("test-certs/1_2.crt");
        Certificate c2 = UpdaterUtil.readCertificate("test-certs/2_2.crt");
        pairs.add(new CertificatePair(c1, c2));
        SimpleUrlExtractor extractor = new SimpleUrlExtractor(new RSADoubleDecryptor(), pairs);
        byte[] bytes = UpdaterUtil.concatArrays(doubleEncryptedBytes.getFirst(), doubleEncryptedBytes.getSecond());
        extractor.extract(bytes, Pattern.compile("http://apollocurrency/ApolloWallet-1.0.8.jar"));
    }
}
