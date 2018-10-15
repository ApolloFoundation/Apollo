/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import static com.apollocurrency.aplwallet.apl.updater.UpdaterConstants.CERTIFICATE_DIRECTORY;
import static com.apollocurrency.aplwallet.apl.updater.UpdaterConstants.CERTIFICATE_SUFFIX;
import static com.apollocurrency.aplwallet.apl.updater.UpdaterConstants.FIRST_DECRYPTION_CERTIFICATE_PREFIX;
import static com.apollocurrency.aplwallet.apl.updater.UpdaterConstants.SECOND_DECRYPTION_CERTIFICATE_PREFIX;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.doCallRealMethod;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.Level;
import com.apollocurrency.aplwallet.apl.Transaction;
import com.apollocurrency.aplwallet.apl.TransactionType;
import com.apollocurrency.aplwallet.apl.UpdateInfo;
import com.apollocurrency.aplwallet.apl.UpdaterDb;
import com.apollocurrency.aplwallet.apl.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.Version;
import com.apollocurrency.aplwallet.apl.updater.downloader.Downloader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import util.TestUtil;
import util.WalletRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
@Ignore
@RunWith(PowerMockRunner.class)
@PrepareForTest(value = {UpdaterUtil.class, AuthorityChecker.class, RSAUtil.class, UpdaterCore.class, UpdaterMediator.class, Downloader.class, PlatformDependentUpdater.class, Unpacker.class, UpdaterDb.class })
public class UpdaterCoreTest {

    private Attachment.UpdateAttachment attachment = Attachment.UpdateAttachment.getAttachment(
            Platform.current(),
            Architecture.current(),
            new DoubleByteArrayTuple(new byte[0], new byte[0]),
            Version.from("1.0.8"),
            new byte[0],
            (byte) 0);
    @Mock
    private UpdaterMediator fakeMediatorInstance;
    @Mock
    private AuthorityChecker fakeCheckerInstance;
    @Mock
    private PlatformDependentUpdater fakePlatformDependentUpdaterInstance;
    @Mock
    private Downloader fakeDownloaderInstance;
    @Mock
    private Unpacker fakeUnpackerInstance;

    private final String decryptedUrl = "http://apollocurrency/ApolloWallet.jar";

    @Before
    public void setUp() throws Exception {
        mockStatic(UpdaterMediator.class);
        BDDMockito.given(UpdaterMediator.getInstance()).willReturn(fakeMediatorInstance);
        mockStatic(Downloader.class);
        BDDMockito.given(Downloader.getInstance()).willReturn(fakeDownloaderInstance);
        mockStatic(Unpacker.class);
        BDDMockito.given(Unpacker.getInstance()).willReturn(fakeUnpackerInstance);
        mockStatic(PlatformDependentUpdater.class);
        BDDMockito.given(PlatformDependentUpdater.getInstance()).willReturn(fakePlatformDependentUpdaterInstance);
        mockStatic(AuthorityChecker.class);
        BDDMockito.given(AuthorityChecker.getInstance()).willReturn(fakeCheckerInstance);
        mockStatic(UpdaterDb.class);
    }

    @Test
    public void testTriggerCriticalUpdate() throws Exception {
        //Prepare testdata
        Version testVersion = Version.from("1.0.8");
        UpdaterCore.UpdateDataHolder holder = new UpdaterCore.UpdateDataHolder(new UpdateTransaction(TransactionType.Update.CRITICAL, 0L, 0L, TestUtil.atm(1L), 0L, 9, attachment), decryptedUrl);

        //Mock dependent classes

        Whitebox.setInternalState(fakeMediatorInstance, "updateInfo", UpdateInfo.getInstance());

        //mock external methods
        when(fakeMediatorInstance, "getBlockchainHeight").thenReturn(10);
        doCallRealMethod().when(fakeMediatorInstance).setUpdateData(true, 10, holder.getTransaction().getHeight(), Level.CRITICAL, testVersion);
        doCallRealMethod().when(fakeMediatorInstance).setUpdateState(any(UpdateInfo.UpdateState.class));

        //spy target class
        UpdaterCore updaterCore = spy(UpdaterCore.getInstance());

        //mock inner private methods
        doReturn(true).when(updaterCore, "tryUpdate", attachment, decryptedUrl, true);

        //call target method
        Whitebox.invokeMethod(updaterCore, "triggerUpdate", holder);

        //verify methods invocations
        verifyPrivate(updaterCore).invoke("tryUpdate", attachment, decryptedUrl, true);
        UpdateInfo info = UpdateInfo.getInstance();
        Assert.assertEquals(testVersion, info.getVersion());
        Assert.assertEquals(9, info.getReceivedHeight());
        Assert.assertEquals(10, info.getEstimatedHeight());
        Assert.assertEquals(Level.CRITICAL, info.getLevel());
        Assert.assertEquals(UpdateInfo.UpdateState.FINISHED, info.getUpdateState());

    }

    @Test
    public void testBadTriggerCriticalUpdate() throws Exception {
        //Prepare testdata
        Version testVersion = Version.from("1.0.8");
        UpdaterCore.UpdateDataHolder holder = new UpdaterCore.UpdateDataHolder(new UpdateTransaction(TransactionType.Update.CRITICAL, 0L, 0L, TestUtil.atm(1L), 0L, 9, attachment), decryptedUrl);

        //Mock dependent classes

        Whitebox.setInternalState(fakeMediatorInstance, "updateInfo", UpdateInfo.getInstance());

        //mock external methods
        doReturn(10).when(fakeMediatorInstance, "getBlockchainHeight");
        doCallRealMethod().when(fakeMediatorInstance).setUpdateData(true, 10, holder.getTransaction().getHeight(), Level.CRITICAL, testVersion);
        doCallRealMethod().when(fakeMediatorInstance).setUpdateState(any(UpdateInfo.UpdateState.class));

        //spy target class
        UpdaterCore updaterCore = spy(UpdaterCore.getInstance());

        //mock inner private methods
        doReturn(false).when(updaterCore, "tryUpdate", attachment, decryptedUrl, true);

        //call target method
        Whitebox.invokeMethod(updaterCore, "triggerUpdate", holder);

        //verify methods invocations
        verifyPrivate(updaterCore).invoke("tryUpdate", attachment, decryptedUrl, true);
        UpdateInfo info = UpdateInfo.getInstance();
        Assert.assertEquals(testVersion, info.getVersion());
        Assert.assertEquals(9, info.getReceivedHeight());
        Assert.assertEquals(10, info.getEstimatedHeight());
        Assert.assertEquals(Level.CRITICAL, info.getLevel());
        Assert.assertEquals(UpdateInfo.UpdateState.REQUIRED_MANUAL_INSTALL, info.getUpdateState());

    }


    @Test
    public void testTriggerImportantUpdate() throws Exception {
        //Prepare testdata
        Version testVersion = Version.from("1.0.8");
        UpdaterCore.UpdateDataHolder holder = new UpdaterCore.UpdateDataHolder(new UpdateTransaction(TransactionType.Update.IMPORTANT, 0L, 0L, TestUtil.atm(1L), 0L, 100, attachment), decryptedUrl);

        //Mock dependent classes

        Whitebox.setInternalState(fakeMediatorInstance, "updateInfo", UpdateInfo.getInstance());

        //mock external methods
        doCallRealMethod().when(fakeMediatorInstance).setUpdateData(anyBoolean(), anyInt(), anyInt(), any(Level.class), any(Version.class));
        doCallRealMethod().when(fakeMediatorInstance).setUpdateState(any(UpdateInfo.UpdateState.class));

        //spy target class
        UpdaterCore updaterCore = spy(UpdaterCore.getInstance());

        //mock inner private methods
        doReturn(true).when(updaterCore, "scheduleUpdate", 101, attachment, decryptedUrl);
        doReturn(101).when(updaterCore, "getUpdateHeightFromType", TransactionType.Update.IMPORTANT);

        //call target method
        Whitebox.invokeMethod(updaterCore, "triggerUpdate", holder);

        //verify methods invocations
        verifyPrivate(updaterCore).invoke("scheduleUpdate", anyInt(), any(Attachment.ImportantUpdate.class), anyString());
        UpdateInfo info = UpdateInfo.getInstance();
        Assert.assertEquals(testVersion, info.getVersion());
        Assert.assertEquals(100, info.getReceivedHeight());
        Assert.assertEquals(101, info.getEstimatedHeight());
        Assert.assertEquals(Level.IMPORTANT, info.getLevel());
        Assert.assertEquals(UpdateInfo.UpdateState.FINISHED, info.getUpdateState());

    }


    @Test
    public void testTriggerMinorUpdate() throws Exception {
        //Prepare testdata
        Version testVersion = Version.from("1.0.8");
        UpdaterCore.UpdateDataHolder holder = new UpdaterCore.UpdateDataHolder(new UpdateTransaction(TransactionType.Update.MINOR, 0L, 0L, TestUtil.atm(1L), 0L, 100, attachment), decryptedUrl);

        //Mock dependent classes

        Whitebox.setInternalState(fakeMediatorInstance, "updateInfo", UpdateInfo.getInstance());

        //mock external methods
        doCallRealMethod().when(fakeMediatorInstance).setUpdateData(anyBoolean(), anyInt(), anyInt(), any(Level.class), any(Version.class));
        doCallRealMethod().when(fakeMediatorInstance).setUpdateState(any(UpdateInfo.UpdateState.class));

        //spy target class
        UpdaterCore updaterCore = spy(UpdaterCore.getInstance());

        //mock inner private methods
        doReturn(true).when(updaterCore, "scheduleUpdate", 101, attachment, decryptedUrl);
        doReturn(101).when(updaterCore, "getUpdateHeightFromType", TransactionType.Update.MINOR);

        //call target method
        Whitebox.invokeMethod(updaterCore, "triggerUpdate", holder);

        //verify
        UpdateInfo info = UpdateInfo.getInstance();
        Assert.assertEquals(testVersion, info.getVersion());
        Assert.assertEquals(100, info.getReceivedHeight());
        Assert.assertEquals(101, info.getEstimatedHeight());
        Assert.assertEquals(Level.MINOR, info.getLevel());
        Assert.assertEquals(UpdateInfo.UpdateState.REQUIRED_START, info.getUpdateState());

    }


    @Test
    public void testVerifyJar() throws Exception {
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
    public void testProcessTransactions() throws Exception {
        doReturn(true).when(fakeCheckerInstance, "verifyCertificates", CERTIFICATE_DIRECTORY);
        mockStatic(UpdaterDb.class);
        Version testVersion = Version.from("1.0.7");
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
        doReturn(fakeWalletUrl).when(RSAUtil.class, "tryDecryptUrl", CERTIFICATE_DIRECTORY, attachment.getUrl(), attachment
                .getAppVersion());
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
        Path fakeJarPath = Paths.get("");
        Path fakeUnpackedDirPath = Paths.get("");

        //Mock dependent classes

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

    @Test
    public void testTryUpdateWithWaiting() throws Exception {
        //Prepare testdata

        Path fakeJarPath = Paths.get("");
        Path fakeUnpackedDirPath = Paths.get("");

        //Mock dependent classes

        //mock external methods
        doReturn(fakeJarPath).when(fakeDownloaderInstance, "tryDownload", decryptedUrl, attachment.getHash());
        doReturn(fakeUnpackedDirPath).when(fakeUnpackerInstance, "unpack", fakeJarPath);
        doNothing().when(fakePlatformDependentUpdaterInstance, "continueUpdate", fakeUnpackedDirPath, attachment.getPlatform());

        //spy target class
        UpdaterCore updaterCore = spy(UpdaterCore.getInstance());

        //mock inner private methods
        doReturn(true).when(updaterCore, "verifyJar", fakeJarPath);
        doNothing().when(updaterCore, "stopForgingAndBlockAcceptance");
        when(fakeMediatorInstance.getBlockchainHeight()).then(new Answer<Integer>() {
            private int counter = 10;

            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                return counter++;
            }
        });
        //call target method
        Object result = Whitebox.invokeMethod(updaterCore, "tryUpdate", attachment, decryptedUrl, true);
        Assert.assertTrue((Boolean) result);

        //verify methods invocations
        verifyPrivate(updaterCore).invoke("verifyJar", fakeJarPath);
        Mockito.verify(fakePlatformDependentUpdaterInstance, Mockito.times(1)).continueUpdate(fakeUnpackedDirPath, attachment.getPlatform());
        Mockito.verify(fakeMediatorInstance, Mockito.times(4)).getBlockchainHeight();
    }

    @Test
    public void testWaitBlocks() throws Exception {
            WalletRunner runner = new WalletRunner();
        try {
            runner.run();
            TimeUnit.SECONDS.sleep(5);
            when(fakeMediatorInstance.getBlockchainHeight()).thenCallRealMethod();
            int startHeight = UpdaterMediator.getInstance().getBlockchainHeight();
            long startTime = System.currentTimeMillis() / 1000;
            Whitebox.invokeMethod(UpdaterCore.getInstance(), "waitBlocks", 1, 70);
            long finishTime = System.currentTimeMillis() / 1000;
            int finishHeight = UpdaterMediator.getInstance().getBlockchainHeight();
            if (finishTime - startTime < 70 && finishHeight - startHeight < 1) {
                Assert.fail("Bad waiting");
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        } finally {
            runner.shutdown();
        }
    }
}
