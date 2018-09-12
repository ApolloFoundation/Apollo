/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.updater.decryption.RSAUtil;
import com.apollocurrency.aplwallet.apl.updater.util.JarGenerator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import static com.apollocurrency.aplwallet.apl.updater.UpdaterUtil.loadResourcePath;

@RunWith(PowerMockRunner.class)
@PrepareForTest(AuthorityCheckerImpl.class)
public class AuthorityCheckerImplTest {
    private AuthorityChecker correctAuthorityChecker = new AuthorityCheckerImpl("rootCA.crt", ".crt", "intermediate.crt", "1_", "2_");
    @Test
    public void testVerifyCertificates() throws Exception {
        Path testRootCAPath = loadResourcePath("certs/rootCA.crt");
        Certificate certificate = UpdaterUtil.readCertificate(testRootCAPath);
        boolean verified = correctAuthorityChecker.verifyCertificates("certs");
        Assert.assertTrue(verified);
    }

    @Test
    public void testNotVerifiedCertificatesWhenIncorrectRootCertificate() throws Exception {

        Path fakeRootCACertificate = loadResourcePath("certs/1_1.crt") ;
        Certificate certificate = UpdaterUtil.readCertificate(fakeRootCACertificate);
        AuthorityChecker incorrectAuthorityChecker = new AuthorityCheckerImpl(certificate, ".crt", "intermediate.crt", "1_", "2_");;

        boolean isVerified = incorrectAuthorityChecker.verifyCertificates("certs");

        Assert.assertFalse(isVerified);
    }

    @Test
    public void testNotVerifiedCertificatesWhenIncorrectPathIntermediateCertificate() {
        AuthorityChecker incorrectAuthorityChecker = new AuthorityCheckerImpl("rootCA.crt", ".crt", "intermediat.crt", "1_", "2_");

        boolean isVerified = incorrectAuthorityChecker.verifyCertificates("certs");

        Assert.assertFalse(isVerified);
    }

    @Test
    public void testVerifyJar() throws Exception {
            Path jarFilePath = Files.createTempFile("apl-test", ".jar");
        try {
            OutputStream jarOutputStream = Files.newOutputStream(jarFilePath);
            Certificate certificate = UpdaterUtil.readCertificate(loadResourcePath("certs/1_2.crt"));
            PrivateKey key = RSAUtil.getPrivateKey("certs/1_2.key");
            JarGenerator generator = new JarGenerator(jarOutputStream, certificate, key);
            generator.generate();
            generator.close();
            jarOutputStream.close();
            correctAuthorityChecker.verifyJarSignature(certificate, jarFilePath);
        }
        finally {
            Files.deleteIfExists(jarFilePath);
        }
    }

    @Test(expected = SecurityException.class)
    public void testVerifyNotSignedJar() throws Exception {
        Path jarFilePath = Files.createTempFile("apl-test", ".jar");
        try {
            OutputStream jarOutputStream = Files.newOutputStream(jarFilePath);
            Certificate certificate = UpdaterUtil.readCertificate(loadResourcePath("certs/1_2.crt"));
            JarGenerator generator = new JarGenerator(jarOutputStream);
            generator.generate();
            generator.close();
            jarOutputStream.close();
            correctAuthorityChecker.verifyJarSignature(certificate, jarFilePath);
        }
        finally {
            Files.deleteIfExists(jarFilePath);
        }
    }
}
