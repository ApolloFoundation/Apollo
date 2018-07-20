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

import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import sun.security.rsa.RSACore;

import javax.crypto.Cipher;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAKey;
import java.security.spec.PKCS8EncodedKeySpec;

import static com.apollocurrency.aplwallet.apl.updater.UpdaterUtil.loadResource;


public class RSAUtil {

    public static DoubleByteArrayTuple doubleEncrypt(PrivateKey privateKey1, PrivateKey privateKey2, byte[] message) throws Exception {
        byte[] firstEncryptedBytes = encrypt(privateKey1, message);
        return secondEncrypt(privateKey2, firstEncryptedBytes);
    }

    public static byte[] doubleDecrypt(PublicKey publicKey1, PublicKey publicKey2, DoubleByteArrayTuple encryptedBytes) throws GeneralSecurityException {
        return decrypt(publicKey2, firstDecrypt(publicKey1, encryptedBytes));
    }

    public static DoubleByteArrayTuple secondEncrypt(PrivateKey privateKey, byte[] encryptedBytes) throws GeneralSecurityException {
        int firstPartLength = encryptedBytes.length - 12;
        byte[] firstEncPart1 = new byte[firstPartLength];
        byte[] firstEncPart2 = new byte[12];

        System.arraycopy(encryptedBytes, 0, firstEncPart1, 0, firstPartLength);
        System.arraycopy(encryptedBytes, firstPartLength, firstEncPart2, 0, 12);

        byte[] secEncPart1 = encrypt(privateKey, firstEncPart1);
        byte[] secEncPart2 = encrypt(privateKey, firstEncPart2);

        return new DoubleByteArrayTuple(secEncPart1, secEncPart2);
    }

    public static byte[] firstDecrypt(PublicKey publicKey, DoubleByteArrayTuple encryptedBytes) throws GeneralSecurityException {
        byte[] firstDecryptedPart1 = decrypt(publicKey, encryptedBytes.getFirst());
        byte[] firstDecryptedPart2 = decrypt(publicKey, encryptedBytes.getSecond());

        byte[] result = new byte[firstDecryptedPart1.length + firstDecryptedPart2.length];

        System.arraycopy(firstDecryptedPart1, 0, result, 0, firstDecryptedPart1.length);
        System.arraycopy(firstDecryptedPart2, 0, result, firstDecryptedPart1.length, firstDecryptedPart2.length);

        return result;
    }



    public static byte[] encrypt(PrivateKey privateKey, byte[] message) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);

        return cipher.doFinal(message);
    }

    public static byte[] decrypt(PublicKey publicKey, byte [] encrypted) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        return cipher.doFinal(encrypted);
    }

    public static byte[] encrypt(String privateKeyPath, byte[] message) throws GeneralSecurityException, IOException, URISyntaxException {
        PrivateKey privateKey = getPrivateKey(privateKeyPath);
        return encrypt(privateKey, message);
    }

    public static byte[] decrypt(String certificatePath, byte [] encrypted) throws GeneralSecurityException, IOException, URISyntaxException {
        PublicKey publicKey = getPublicKeyFromCertificate(certificatePath);
        return decrypt(publicKey, encrypted);
    }

    public static PrivateKey getPrivateKey(String path) throws IOException, GeneralSecurityException, URISyntaxException {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PEMParser pem = new PEMParser(new FileReader(loadResource(path)));
        PEMKeyPair pair = (PEMKeyPair) pem.readObject();
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(pair.getPrivateKeyInfo().getEncoded());

        return kf.generatePrivate(spec);
    }


    public static PublicKey getPublicKeyFromCertificate(String filename) throws CertificateException, IOException, URISyntaxException {
        Certificate certificate = UpdaterUtil.readCertificate(loadResource(filename).toPath());
        return certificate.getPublicKey();
    }

    public static int maxEncryptionLength(RSAKey key) {
        return RSACore.getByteLength(key) - 11;
    }
    public static int keyLength(RSAKey key) {
        return RSACore.getByteLength(key);
    }

}
