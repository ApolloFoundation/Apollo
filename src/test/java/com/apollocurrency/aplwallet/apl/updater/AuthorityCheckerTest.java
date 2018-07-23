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
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import static com.apollocurrency.aplwallet.apl.updater.UpdaterUtil.loadResourcePath;
import static org.powermock.api.mockito.PowerMockito.doReturn;

@RunWith(PowerMockRunner.class)
@PrepareForTest(AuthorityChecker.class)
public class AuthorityCheckerTest {

    @Test
    public void testVerifyCertificates() throws Exception {
        AuthorityChecker spy = PowerMockito.spy(AuthorityChecker.getInstance());
        Path testRootCAPath = loadResourcePath("certs/rootCA.crt");
        doReturn(testRootCAPath).when(spy, "downloadCACertificate");

        boolean isVerified = spy.verifyCertificates("certs");

        Assert.assertTrue(isVerified);

    }

    @Test
    public void testNotVerifiedCertificates() throws Exception {
        AuthorityChecker spy = PowerMockito.spy(AuthorityChecker.getInstance());
        Path fakeRootCACertificate = loadResourcePath("certs/1_1.crt");
        doReturn(fakeRootCACertificate).when(spy, "downloadCACertificate");

        boolean isVerified = spy.verifyCertificates("certs");

        Assert.assertFalse(isVerified);
    }

    @Test
    public void testVerifyJar() throws Exception {
            AuthorityChecker checker = PowerMockito.spy(AuthorityChecker.getInstance());
            Path jarFilePath = Files.createTempFile("apl-test", ".jar");
        try {
            OutputStream jarOutputStream = Files.newOutputStream(jarFilePath);
            Certificate certificate = UpdaterUtil.readCertificate(loadResourcePath("certs/1_2.crt"));
            PrivateKey key = RSAUtil.getPrivateKey("certs/1_2.key");
            JarGenerator generator = new JarGenerator(jarOutputStream, certificate, key);
            generator.generate();
            generator.close();
            jarOutputStream.close();
            checker.verifyJarSignature(certificate, jarFilePath);
            Files.delete(jarFilePath);
        }
        catch (Exception e) {
            Files.deleteIfExists(jarFilePath);
            throw e;
        }
    }

    @Test(expected = SecurityException.class)
    public void testVerifyNotSignedJar() throws Exception {
        AuthorityChecker checker = PowerMockito.spy(AuthorityChecker.getInstance());
        Path jarFilePath = Files.createTempFile("apl-test", ".jar");
        try {
            OutputStream jarOutputStream = Files.newOutputStream(jarFilePath);
            Certificate certificate = UpdaterUtil.readCertificate(loadResourcePath("certs/1_2.crt"));
            JarGenerator generator = new JarGenerator(jarOutputStream);
            generator.generate();
            generator.close();
            jarOutputStream.close();
            checker.verifyJarSignature(certificate, jarFilePath);
            Files.delete(jarFilePath);
        }
        catch (Exception e) {
            Files.deleteIfExists(jarFilePath);
            throw e;
        }
    }
}
