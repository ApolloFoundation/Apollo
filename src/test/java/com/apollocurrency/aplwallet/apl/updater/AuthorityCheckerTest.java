package com.apollocurrency.aplwallet.apl.updater;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.nio.file.Path;

import static org.powermock.api.mockito.PowerMockito.doReturn;

@RunWith(PowerMockRunner.class)
@PrepareForTest(AuthorityChecker.class)
public class AuthorityCheckerTest {

    @Test
    public void verifyJar() throws Exception {
        AuthorityChecker spy = PowerMockito.spy(AuthorityChecker.getInstance());
        Path testRootCAPath = RSAUtil.loadResource("certs/rootCA.crt").toPath();
        doReturn(testRootCAPath).when(spy, "downloadCACertificate");

        boolean isVerified = spy.verifyCertificates("certs");

        Assert.assertTrue(isVerified);

    }


}
