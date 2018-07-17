package com.apollocurrency.aplwallet.apl.updater;

import org.junit.Assert;
import org.junit.Test;

import java.security.PrivateKey;
import java.security.PublicKey;

import static com.apollocurrency.aplwallet.apl.updater.RSAUtil.*;

public class RSAUtilTest {
    @Test
    public void testEncryptAndDecrypt() throws Exception {
        PublicKey pubKey = getPublicKeyFromCertificate("1_self_signed.cert");
        PrivateKey privateKey = getPrivateKey("1_private.key");

        // encrypt the message
        String expectedMessage = "This is a secret message";
        byte[] encrypted = encrypt(privateKey, expectedMessage.getBytes());
        System.out.println("Encrypted message in hex:");

        // decrypt the message
        System.out.println("Decrypted message:");
        byte[] secret = decrypt(pubKey, encrypted);
        Assert.assertEquals(expectedMessage, new String(secret));
    }

    @Test
    public void doubleEncryptAndDecrypt() throws Exception {
        PublicKey pubKey1 = getPublicKeyFromCertificate("1_self_signed.cert");
        PrivateKey privateKey1 = getPrivateKey("1_private.key");

        PublicKey pubKey2 = getPublicKeyFromCertificate("2_self_signed.cert");
        PrivateKey privateKey2 = getPrivateKey("2_private.key");

        // encrypt the message
        String expectedMessage = "This is a secret message";
        byte[] firstEncrypted = encrypt(privateKey1, expectedMessage.getBytes());

        byte[] encPart1 = new byte[firstEncrypted.length - 12];
        byte[] encPart2 = new byte[12];

        System.arraycopy(firstEncrypted, 0, encPart1, 0, 500);
        System.arraycopy(firstEncrypted, 500, encPart2, 0, 12);


        byte[] secEncPart1 = encrypt(privateKey2, encPart1);
        byte[] secEncPart2 = encrypt(privateKey2, encPart2);

        // decrypt the message

        byte[] firDecrPart1 = decrypt(pubKey2, secEncPart1);
        byte[] firDecrPart2 = decrypt(pubKey2, secEncPart2);

        byte[] result = new byte[firstEncrypted.length];

        System.arraycopy(firDecrPart1, 0, result, 0, firDecrPart1.length);
        System.arraycopy(firDecrPart2, 0, result, firDecrPart1.length, firDecrPart2.length);

        byte[] secondSecret = decrypt(pubKey1, result);
        String actual = new String(secondSecret);

//        for (int i = 0; i < splittedDecryptedData2.length; i++) {
//            actual += new String(splittedDecryptedData2[i]);
//        }
        System.out.println("Decrypted message:");
        System.out.println(actual);

        Assert.assertEquals(expectedMessage, actual);
    }
}
