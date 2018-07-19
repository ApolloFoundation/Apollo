package com.apollocurrency.aplwallet.apl.updater;

import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Random;

public class JarGenerator {
    private SimpleSignedJar signedJar;
    private static final String KEY_ALIAS = "test";
    private static final String KEY_PASSWORD = "test";


    public JarGenerator(OutputStream outputStream, Certificate certificate, PrivateKey privateKey) throws GeneralSecurityException, IOException {
        KeyStore keyStore = initKeyStore(certificate, privateKey);
        this.signedJar = new SimpleSignedJar(outputStream, keyStore, KEY_ALIAS,  KEY_PASSWORD);
    }

    public JarGenerator(OutputStream outputStream) {
//        this.signedJar = new SimpleJar(outputStream);
    }


    private KeyStore initKeyStore(Certificate certificate, PrivateKey privateKey) throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setEntry(KEY_ALIAS, new KeyStore.PrivateKeyEntry(privateKey, new Certificate[] {certificate}), new KeyStore.PasswordProtection(KEY_PASSWORD.toCharArray()));
        return keyStore;
    }

    public void generate() throws IOException {
        signedJar.addManifestAttribute("Main-Class", "com.test.MainClass");
        signedJar.addManifestAttribute("Application-Name", "Test-app");
        signedJar.addManifestAttribute("Permissions", "all-permissions");
        signedJar.addFileContents("com/test/MainClass.class", randomBytes(4096));
        signedJar.addFileContents("com/test/AnotherClass.class", randomBytes(2123));
        signedJar.addFileContents("com/test/AnotherClass2.class", randomBytes(7654));
    }

    public void close() throws IOException {
        signedJar.close();
    }

    private byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        Random random = new Random();
        random.nextBytes(bytes);
        return bytes;
    }
}
