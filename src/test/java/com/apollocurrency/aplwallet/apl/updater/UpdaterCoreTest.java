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

import com.apollocurrency.aplwallet.apl.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.util.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.HashSet;
import java.util.Set;

import static com.apollocurrency.aplwallet.apl.updater.UpdaterConstants.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("com.apollocurrency.aplwallet.apl.util.Logger")
@PrepareForTest(UpdaterUtil.class)
public class UpdaterCoreTest {
    @Test
    public void testTriggerUpdate() throws Exception {
    }

    @Test
    public void testTryDecryptUrl() {
    }

    @Mock
    private UpdaterMediator fakeMediatorInstance;

    @Test
    public void testVerifyJar() throws Exception {
        Class<?> clazz = Class.forName("com.apollocurrency.aplwallet.apl.UpdaterMediator$UpdaterMediatorHolder");
        Whitebox.setInternalState(clazz, "INSTANCE", fakeMediatorInstance);
        Path signedJar = Files.createTempFile("test-verifyjar", ".jar");
        try {
            Set<Certificate> certificates = new HashSet<>();
            certificates.add(UpdaterUtil.readCertificate("certs/1_1.crt"));
            certificates.add(UpdaterUtil.readCertificate("certs/1_2.crt"));
            certificates.add(UpdaterUtil.readCertificate("certs/2_1.crt"));
            certificates.add(UpdaterUtil.readCertificate("certs/2_2.crt"));
            PrivateKey privateKey = RSAUtil.getPrivateKey("certs/2_2.key");
            JarGenerator generator = new JarGenerator(Files.newOutputStream(signedJar), UpdaterUtil.readCertificate("certs/2_2.crt"), privateKey);
            generator.generate();
            generator.close();

            PowerMockito.mockStatic(UpdaterUtil.class);
            when(UpdaterUtil.readCertificates(CERTIFICATE_DIRECTORY, CERTIFICATE_SUFFIX, FIRST_DECRYPTION_CERTIFICATE_PREFIX, SECOND_DECRYPTION_CERTIFICATE_PREFIX)).thenReturn(certificates);
            PowerMockito.mockStatic(Logger.class);
            Object result = Whitebox.invokeMethod(UpdaterCore.getInstance(), "verifyJar", signedJar);
            Assert.assertTrue(Boolean.parseBoolean(result.toString()));
        }
        finally {
            Files.deleteIfExists(signedJar);
        }
    }

    @Test
    public void testScheduleUpdate() {

    }

    @Test
    public void testProcessTransactions() {

    }

    @Test
    public void testTryUpdate() {

    }
}
