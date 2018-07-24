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
import com.apollocurrency.aplwallet.apl.updater.downloader.Downloader;
import com.apollocurrency.aplwallet.apl.util.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import util.TestUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.*;

import static com.apollocurrency.aplwallet.apl.updater.UpdaterConstants.*;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("com.apollocurrency.aplwallet.apl.util.Logger")
@PrepareForTest({UpdaterUtil.class, AuthorityChecker.class, RSAUtil.class, UpdaterCore.class, UpdaterMediator.class, Downloader.class, PlatformDependentUpdater.class, Unpacker.class})
public class UpdaterCoreTest {
    @Mock
    private UpdaterMediator fakeMediatorInstance;
    @Mock
    private AuthorityChecker fakeCheckerInstance;

    @Mock
    private Downloader fakeDownloaderInstance;

    @Mock
    private Unpacker fakeUnpackerInstance;

    @Mock
    private PlatformDependentUpdater fakePlatformDependentUpdaterInstance;


    @Test
    public void testTriggerUpdate() throws Exception {
        //Prepare testdata
        Version testVersion = Version.from("1.0.8");
        Attachment.UpdateAttachment attachment = Attachment.UpdateAttachment.getAttachment(
                Platform.current(),
                Architecture.current(),
                new DoubleByteArrayTuple(new byte[0], new byte[0]),
                testVersion,
                new byte[0],
                (byte) 0);
        String decryptedUrl = "http://apollocurrency/ApolloWallet.jar";
        UpdaterCore.UpdateDataHolder holder = new UpdaterCore.UpdateDataHolder(new UpdateTransaction(TransactionType.Update.CRITICAL, 0L, 0L, TestUtil.atm(1L), 0L, 9, attachment), decryptedUrl);

        //Mock dependent classes

        mockStatic(UpdaterMediator.class);
        BDDMockito.given(UpdaterMediator.getInstance()).willReturn(fakeMediatorInstance);
        Whitebox.setInternalState(fakeMediatorInstance, "updateInfo", UpdateInfo.getInstance());

        mockStatic(Downloader.class);
        BDDMockito.given(Downloader.getInstance()).willReturn(fakeDownloaderInstance);


        mockStatic(Unpacker.class);
        BDDMockito.given(Unpacker.getInstance()).willReturn(fakeUnpackerInstance);

        mockStatic(PlatformDependentUpdater.class);
        BDDMockito.given(PlatformDependentUpdater.getInstance()).willReturn(fakePlatformDependentUpdaterInstance);

        //mock external methods

        doReturn(10).when(fakeMediatorInstance, "getBlockchainHeight");
        doCallRealMethod().when(fakeMediatorInstance).setUpdateData(true, 10, holder.getTransaction().getHeight(),Level.CRITICAL, testVersion);
        doCallRealMethod().when(fakeMediatorInstance).setUpdateState(any(UpdateInfo.UpdateState.class));

        //spy target class
        UpdaterCore updaterCore = spy(UpdaterCore.getInstance());

        //mock inner private methods
        doReturn(true).when(updaterCore, "tryUpdate", attachment, decryptedUrl);

        //call target method
        updaterCore.triggerUpdate(holder);

        //verify methods invocations
        verifyPrivate(updaterCore).invoke("tryUpdate", attachment, decryptedUrl);
        UpdateInfo info = UpdateInfo.getInstance();
        Assert.assertEquals(testVersion, info.getVersion());
        Assert.assertEquals(9, info.getReceivedHeight());
        Assert.assertEquals(10, info.getEstimatedHeight());
        Assert.assertEquals(Level.CRITICAL, info.getLevel());
        Assert.assertEquals(UpdateInfo.UpdateState.FINISHED, info.getUpdateState());

    }

    @BeforeClass
    public static void init() {
        mockStatic(Logger.class);
    }

    @Test
    public void testVerifyJar() throws Exception {
        mockStatic(UpdaterMediator.class);
        BDDMockito.given(UpdaterMediator.getInstance()).willReturn(fakeMediatorInstance);
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

            mockStatic(UpdaterUtil.class);
            when(UpdaterUtil.readCertificates(CERTIFICATE_DIRECTORY, CERTIFICATE_SUFFIX, FIRST_DECRYPTION_CERTIFICATE_PREFIX, SECOND_DECRYPTION_CERTIFICATE_PREFIX)).thenReturn(certificates);
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

    //TODO create few tests
    @Test
    public void testProcessTransactions() throws Exception {
        mockStatic(AuthorityChecker.class);
        BDDMockito.given(AuthorityChecker.getInstance()).willReturn(fakeCheckerInstance);
        doReturn(true).when(fakeCheckerInstance, "verifyCertificates", CERTIFICATE_DIRECTORY);

        Version testVersion = Version.from("1.0.7");
        mockStatic(UpdaterMediator.class);
        BDDMockito.given(UpdaterMediator.getInstance()).willReturn(fakeMediatorInstance);
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
        String fakeWalletUrl = "http://apollocurrency/ApolloWallet-" + testVersion + ".jar";
        doReturn(fakeWalletUrl).when(RSAUtil.class, "tryDecryptUrl", attachment.getUrl(), attachment
                .getAppVersion(), "(http)|(https)://.+/ApolloWallet-%s.jar");
        Thread mock = mock(Thread.class);
        doNothing().when(mock, "start");
        whenNew(Thread.class).withAnyArguments().thenReturn(mock);
        List<Transaction> randomTransactions = getRandomTransactions(6);
        UpdateTransaction updateTransaction = new UpdateTransaction(TransactionType.Update.CRITICAL, 0, 0, TestUtil.atm
                (1L), 0, attachment);
        randomTransactions.add(updateTransaction);

        when(fakeMediatorInstance, "isUpdateTransaction", updateTransaction).thenReturn(true);
        Whitebox.invokeMethod(UpdaterCore.getInstance(), "processTransactions", randomTransactions);
        Mockito.verify(mock, Mockito.times(1)).start();
        UpdaterCore.UpdateDataHolder updateDataHolder = Whitebox.getInternalState(UpdaterCore.getInstance(), "updateDataHolder");
        Assert.assertEquals(updateTransaction, updateDataHolder.getTransaction());
        Assert.assertEquals(fakeWalletUrl, updateDataHolder.getDecryptedUrl());
    }

    private List<Transaction> getRandomTransactions(int numberOfTransactions) {
        List<Transaction> transactions = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < numberOfTransactions; i++) {
            Transaction transaction = new SimpleTransactionImpl(TransactionType.findTransactionType((byte) random.nextInt(8), (byte) 0), random.nextLong(), random.nextLong(), TestUtil.atm(random.nextInt(10_000)), TestUtil.atm(random.nextInt(10_000)));
            transactions.add(transaction);
        }
        return transactions;
    }

    //TODO make public method calls mocking via when...return
    @Test
    public void testTryUpdate() throws Exception {
        //Prepare testdata
        Attachment.UpdateAttachment attachment = Attachment.UpdateAttachment.getAttachment(
                Platform.current(),
                Architecture.current(),
                new DoubleByteArrayTuple(new byte[0], new byte[0]),
                Version.from("1.0.8"),
                new byte[0],
                (byte) 0);
        String decryptedUrl = "http://apollocurrency/ApolloWallet.jar";
        Path fakeJarPath = Paths.get("");
        Path fakeUnpackedDirPath = Paths.get("");

        //Mock dependent classes

        mockStatic(UpdaterMediator.class);
        BDDMockito.given(UpdaterMediator.getInstance()).willReturn(fakeMediatorInstance);

        mockStatic(Downloader.class);
        BDDMockito.given(Downloader.getInstance()).willReturn(fakeDownloaderInstance);

        mockStatic(Unpacker.class);
        BDDMockito.given(Unpacker.getInstance()).willReturn(fakeUnpackerInstance);

        mockStatic(PlatformDependentUpdater.class);
        BDDMockito.given(PlatformDependentUpdater.getInstance()).willReturn(fakePlatformDependentUpdaterInstance);

        //mock external methods
        doReturn(fakeJarPath).when(fakeDownloaderInstance, "tryDownload", decryptedUrl, attachment.getHash());
        doReturn(fakeUnpackedDirPath).when(fakeUnpackerInstance, "unpack", fakeJarPath);
        doNothing().when(fakePlatformDependentUpdaterInstance, "continueUpdate", fakeUnpackedDirPath, attachment.getPlatform());

        //spy target class
        UpdaterCore updaterCore = spy(UpdaterCore.getInstance());

        //mock inner private methods
        doReturn(true).when(updaterCore, "verifyJar", fakeJarPath);
        doNothing().when(updaterCore, "stopForgingAndBlockAcceptance");


        //call target method
        Object result = Whitebox.invokeMethod(updaterCore, "tryUpdate", attachment, decryptedUrl);
        Assert.assertTrue((Boolean) result);

        //verify methods invocations
        verifyPrivate(updaterCore).invoke("verifyJar", fakeJarPath);
        Mockito.verify(fakePlatformDependentUpdaterInstance, Mockito.times(1)).continueUpdate(fakeUnpackedDirPath, attachment.getPlatform());
    }
}
