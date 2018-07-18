/*
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

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

        String expectedMessage = "This is a secret message!";

        DoubleByteArrayTuple doubleEncryptedMessageBytes = doubleEncrypt(privateKey1, privateKey2, expectedMessage.getBytes());

        byte[] doubleDecryptedMessageBytes = doubleDecrypt(pubKey2, pubKey1, doubleEncryptedMessageBytes);

        String actual = new String(doubleDecryptedMessageBytes);

        System.out.println("Decrypted message:");
        System.out.println(actual);

        Assert.assertEquals(expectedMessage, actual);
    }
}
