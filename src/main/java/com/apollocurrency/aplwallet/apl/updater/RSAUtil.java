package com.apollocurrency.aplwallet.apl.updater;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;

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
import java.security.spec.PKCS8EncodedKeySpec;


public class RSAUtil {

    public static byte[] encrypt(PrivateKey privateKey, byte[] message) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);

        return cipher.doFinal(message);
    }

    public static byte[] decrypt(PublicKey publicKey, byte [] encrypted) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        return cipher.doFinal(encrypted);
    }

    public static PrivateKey getPrivateKey(String filename) throws IOException, GeneralSecurityException, URISyntaxException {
        return getPrivateKeyFromPath(filename);
    }

    public static PrivateKey getPrivateKeyFromPath(String path) throws IOException, GeneralSecurityException, URISyntaxException {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PEMParser pem = new PEMParser(new FileReader(loadResource(path)));
        PrivateKeyInfo bcKeyPair = (PrivateKeyInfo ) pem.readObject();
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bcKeyPair.getEncoded());

        return kf.generatePrivate(spec);
    }


    public static PublicKey getPublicKeyFromCertificate(String filename) throws CertificateException, IOException, URISyntaxException {
        Certificate certificate = UpdaterUtil.readCertificate(loadResource(filename).toPath());
        return certificate.getPublicKey();
    }

    public static File loadResource(String fileName) throws URISyntaxException {
        return new File(RSAUtil.class.getClassLoader().getResource(fileName).toURI());
    }
}
