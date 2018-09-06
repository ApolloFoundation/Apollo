/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.Version;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.apollocurrency.aplwallet.apl.util.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import java.security.PrivateKey;
import java.security.PublicKey;

import static com.apollocurrency.aplwallet.apl.updater.RSAUtil.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@SuppressStaticInitializationFor("com.apollocurrency.aplwallet.apl.util.Logger")
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.crypto.*" })
public class RSAUtilTest {
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
        mockStatic(Logger.class);
        PublicKey pubKey1 = getPublicKeyFromCertificate("certs/1_2.crt");
        PrivateKey privateKey1 = getPrivateKey("certs/1_2.key");

        PublicKey pubKey2 = getPublicKeyFromCertificate("certs/2_2.crt");
        PrivateKey privateKey2 = getPrivateKey("certs/2_2.key");

        String expectedMessage = "http://apollocurrency/ApolloWallet-1.0.8.jar";
        DoubleByteArrayTuple doubleEncryptedBytes = RSAUtil.doubleEncrypt(privateKey1, privateKey2, expectedMessage.getBytes());

        String url = RSAUtil.tryDecryptUrl("certs", doubleEncryptedBytes, Version.from("1.0.8"));
        Assert.assertNotNull(url);
        Assert.assertEquals(expectedMessage, url);
    }
}
