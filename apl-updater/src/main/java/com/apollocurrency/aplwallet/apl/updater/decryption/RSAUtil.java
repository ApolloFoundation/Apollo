/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.decryption;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.updater.UpdaterUtil;
import com.apollocurrency.aplwallet.apl.util.DoubleByteArrayTuple;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAKey;
import java.security.spec.PKCS8EncodedKeySpec;
import javax.crypto.Cipher;


public class RSAUtil {
    private static final Logger LOG = getLogger(RSAUtil.class);

    private static final String URL_TEMPLATE = "((http)|(https))://.+/Apollo.*-%s.jar";

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

        if (firstDecryptedPart1 == null || firstDecryptedPart2 == null) return null;

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

    public static byte[] decrypt(PublicKey publicKey, byte[] encrypted) throws GeneralSecurityException {
        if (encrypted == null) return null;
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        return cipher.doFinal(encrypted);
    }

    public static byte[] encrypt(String privateKeyPath, byte[] message) throws GeneralSecurityException, IOException, URISyntaxException {
        PrivateKey privateKey = getPrivateKey(privateKeyPath);
        return encrypt(privateKey, message);
    }

    public static byte[] decrypt(String certificatePath, byte[] encrypted) throws GeneralSecurityException {
        try {
            PublicKey publicKey = getPublicKeyFromCertificate(certificatePath);
            return decrypt(publicKey, encrypted);
        }
        catch (CertificateException | IOException | URISyntaxException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static PrivateKey getPrivateKey(String path) throws IOException, GeneralSecurityException, URISyntaxException {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        URL resource = UpdaterUtil.getResource(path);
        Object keyObject = null;
        if (resource == null) {
            Path pathToFile = Paths.get(path);
            PEMParser   pem = new PEMParser(new InputStreamReader(Files.newInputStream(pathToFile)));
            keyObject = pem.readObject();
        } else {
            PEMParser pem = new PEMParser(new InputStreamReader(resource.openStream()));
            keyObject = pem.readObject();
        }
        byte[] privateKeyEncoded;
        if (keyObject instanceof PEMKeyPair) {
            PEMKeyPair pair = (PEMKeyPair) keyObject;
            privateKeyEncoded = pair.getPrivateKeyInfo().getEncoded();
        } else if (keyObject instanceof PrivateKeyInfo) {
            privateKeyEncoded = ((PrivateKeyInfo) keyObject).getEncoded();
        } else {
            throw new SecurityException("Unable to get rsa privateKey");
        }
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privateKeyEncoded);

        return kf.generatePrivate(spec);
    }


    public static PublicKey getPublicKeyFromCertificate(String filename) throws CertificateException, IOException, URISyntaxException {
        Certificate certificate = UpdaterUtil.readCertificate(filename);
        return certificate.getPublicKey();
    }

    public static int maxEncryptionLength(RSAKey key) {
        return keyLength(key)-11;
    }

    public static int keyLength(RSAKey key) {
        int n = key.getModulus().bitLength();
        int res = (n + 7) >> 3;
        return res;
    }


}
