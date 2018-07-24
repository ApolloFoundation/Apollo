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

import com.apollocurrency.aplwallet.apl.*;
import com.apollocurrency.aplwallet.apl.util.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import util.TestUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.*;

import static com.apollocurrency.aplwallet.apl.updater.UpdaterConstants.*;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("com.apollocurrency.aplwallet.apl.util.Logger")
@PrepareForTest({UpdaterUtil.class, AuthorityChecker.class, RSAUtil.class, UpdaterCore.class})
public class UpdaterCoreTest {
    @Mock
    private UpdaterMediator fakeMediatorInstance;
    @Mock
    private AuthorityChecker checker;
    @Test
    public void testTriggerUpdate() throws Exception {
    }

    @Test
    public void testTryDecryptUrl() {
    }


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
    public void testProcessTransactions() throws Exception {
        mockStatic(AuthorityChecker.class);
        BDDMockito.given(AuthorityChecker.getInstance()).willReturn(checker);
        doReturn(true).when(checker, "verifyCertificates", CERTIFICATE_DIRECTORY);

        Version testVersion = Version.from("1.0.7");
        Class<?> clazz = Class.forName("com.apollocurrency.aplwallet.apl.UpdaterMediator$UpdaterMediatorHolder");
        Whitebox.setInternalState(clazz, "INSTANCE", fakeMediatorInstance);
        when(fakeMediatorInstance, "getWalletVersion").thenReturn(testVersion);

        Platform currentPlatform = Platform.current();
        Architecture currentArchitecture = Architecture.current();
        Attachment.UpdateAttachment attachment = Attachment.UpdateAttachment.getAttachment(
                currentPlatform,
                currentArchitecture,
                new DoubleByteArrayTuple(new byte[0], new byte[0]),
                Version.from("1.0.8"),
                new byte[0],
                (byte) 0);

        mockStatic(RSAUtil.class);
        doReturn("http://apollocurrency/ApolloWallet-" + testVersion + ".jar").when(RSAUtil.class, "tryDecryptUrl", attachment.getUrl(), attachment
                .getAppVersion(), "(http)|(https)://.+/ApolloWallet-%s.jar");
        Thread mock = mock(Thread.class);
        doNothing().when(mock, "start");
        whenNew(Thread.class).withAnyArguments().thenReturn(mock);
        List<Transaction> randomTransactions = mockRandomTransaction(6);
        UpdateTransaction e = new UpdateTransaction(TransactionType.Update.CRITICAL, 0, 0, TestUtil.atm
                (1L), 0, attachment);
        randomTransactions.add(e);

        when(fakeMediatorInstance, "isUpdateTransaction", e).thenReturn(true);
        Whitebox.invokeMethod(UpdaterCore.getInstance(), "processTransactions", randomTransactions);
        Mockito.verify(mock, Mockito.times(1)).start();
    }

    List<Transaction> mockRandomTransaction(int numberOfTransactions) {
        List<Transaction> transactions = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < numberOfTransactions; i++) {
            Transaction transaction = new SimpleTransactionImpl(TransactionType.findTransactionType((byte) random.nextInt(8), (byte) 0), random.nextLong(), random.nextLong(), TestUtil.atm(random.nextInt(10_000)), TestUtil.atm(random.nextInt(10_000)));
            transactions.add(transaction);
        }
        return transactions;
    }

    @Test
    public void testTryUpdate() {

    }
}
