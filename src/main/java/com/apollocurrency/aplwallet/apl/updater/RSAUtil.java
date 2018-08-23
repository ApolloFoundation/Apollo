/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.Version;
import com.apollocurrency.aplwallet.apl.util.Logger;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import sun.security.rsa.RSACore;

import javax.crypto.Cipher;
import java.io.File;
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
import java.util.Set;

import static com.apollocurrency.aplwallet.apl.updater.UpdaterUtil.loadResource;


public class RSAUtil {
    private static final String URL_TEMPLATE = "((http)|(https))://.+/Apollo-%s.jar";

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

        try
        {
           byte[] decrypted = cipher.doFinal(encrypted);
           return decrypted;
        }
        catch(javax.crypto.BadPaddingException e)
        {
            return null;
        }

    }

    public static byte[] encrypt(String privateKeyPath, byte[] message) throws GeneralSecurityException, IOException, URISyntaxException {
        PrivateKey privateKey = getPrivateKey(privateKeyPath);
        return encrypt(privateKey, message);
    }

    public static byte[] decrypt(String certificatePath, byte[] encrypted) throws GeneralSecurityException, IOException, URISyntaxException {
        PublicKey publicKey = getPublicKeyFromCertificate(certificatePath);
        return decrypt(publicKey, encrypted);
    }

    public static PrivateKey getPrivateKey(String path) throws IOException, GeneralSecurityException, URISyntaxException {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        File file = loadResource(path);
        PEMParser pem = new PEMParser(new FileReader(file));
        Object keyObject = pem.readObject();
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
        Certificate certificate = UpdaterUtil.readCertificate(loadResource(filename).toPath());
        return certificate.getPublicKey();
    }

    public static int maxEncryptionLength(RSAKey key) {
        return RSACore.getByteLength(key) - 11;
    }

    public static int keyLength(RSAKey key) {
        return RSACore.getByteLength(key);
    }

    public static void main(String[] args) {
        tryDecryptUrl("conf/certs", new DoubleByteArrayTuple(new byte[0], new byte[0]), Version.from("1.0.8"));
    }
    public static String tryDecryptUrl(String certificateDirectory, DoubleByteArrayTuple encryptedUrl, Version updateVersion) {
        Set<UpdaterUtil.CertificatePair> certificatePairs;
        try {
            certificatePairs = UpdaterUtil.buildCertificatePairs(certificateDirectory);
            for (UpdaterUtil.CertificatePair pair : certificatePairs) {
                try {
                    String urlString = new String(RSAUtil.doubleDecrypt(pair.getFirstCertificate().getPublicKey(), pair.getSecondCertificate().getPublicKey
                            (), encryptedUrl));
                    if (urlString.matches(String.format(URL_TEMPLATE, updateVersion.toString()))) {
                        Logger.logDebugMessage("Decrypted url using: " + pair);
                        return urlString;
                    }
                }
                catch (Exception e) {
                      Logger.logDebugMessage("Cannot decrypt url.");
                }
            }
        }
        catch (IOException | CertificateException | URISyntaxException e) {
            Logger.logErrorMessage("Cannot read or load certificate", e);
        }
        return null;
    }

}
