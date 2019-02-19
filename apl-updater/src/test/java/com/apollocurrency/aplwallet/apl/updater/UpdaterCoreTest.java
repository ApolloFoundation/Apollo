/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.Update;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.UpdateAttachment;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.udpater.intfce.Level;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdateData;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdateInfo;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterCore;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.updater.core.UpdaterCoreImpl;
import com.apollocurrency.aplwallet.apl.updater.core.UpdaterFactory;
import com.apollocurrency.aplwallet.apl.updater.pdu.PlatformDependentUpdater;
import com.apollocurrency.aplwallet.apl.updater.service.UpdaterService;
import com.apollocurrency.aplwallet.apl.util.Architecture;
import com.apollocurrency.aplwallet.apl.util.DoubleByteArrayTuple;
import com.apollocurrency.aplwallet.apl.util.Listener;
import com.apollocurrency.aplwallet.apl.util.Platform;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdaterCoreTest {

    private UpdateAttachment attachment = UpdateAttachment.getAttachment(
            Platform.current(),
            Architecture.current(),
            new DoubleByteArrayTuple(new byte[0], new byte[0]),
            new Version("1.0.8"),
            new byte[0],
            (byte) 0);
    @Mock
    private UpdaterMediator updaterMediatorInstance;
    @Mock
    private PlatformDependentUpdater fakePlatformDependentUpdaterInstance;
    @Mock
    private UpdaterService updaterService;
    @Mock
    private UpdaterFactory updaterFactory;
    @Mock
    private UpdateTransactionVerifier transactionVerifier;

    private final String decryptedUrl = "http://apollocurrency/ApolloWallet.jar";

    @Before
    public void setUp() throws Exception {

    }

//   UpdaterCoreImpl Init tests

    @Test
    public void testInitNotUpdatedTransaction() throws Exception {
        SimpleTransaction mockTransaction = new SimpleTransaction(0, Update.CRITICAL);
        mockTransaction.setAttachment(attachment);
        UpdateTransaction updateTransaction = new UpdateTransaction(mockTransaction, false);
        when(updaterService.getLast()).thenReturn(updateTransaction);
        when(transactionVerifier.process(mockTransaction)).thenReturn(new UpdateData(mockTransaction, decryptedUrl));
        UpdaterCore updaterCore = new UpdaterCoreImpl(updaterService, updaterMediatorInstance, updaterFactory, transactionVerifier);
        UpdaterCore spy = spy(updaterCore);

        spy.init();
        TimeUnit.SECONDS.sleep(2);
        verify(spy, times(1)).startUpdate(any(UpdateData.class));
        verify(updaterService, times(1)).getLast();
        verify(transactionVerifier, times(1)).process(any(Transaction.class));
        verify(updaterMediatorInstance, never()).addUpdateListener(any(Listener.class));
    }

    @Test
    public void testInitNotUpdatedMinorTransaction() throws Exception {
        SimpleTransaction mockTransaction = new SimpleTransaction(0, Update.MINOR);
        mockTransaction.setAttachment(attachment);
        UpdateTransaction updateTransaction = new UpdateTransaction(mockTransaction, false);
        when(updaterService.getLast()).thenReturn(updateTransaction);
        when(transactionVerifier.process(mockTransaction)).thenReturn(new UpdateData(mockTransaction, decryptedUrl));
        UpdaterCore updaterCore = new UpdaterCoreImpl(updaterService, updaterMediatorInstance, updaterFactory, transactionVerifier);
        UpdaterCore spy = spy(updaterCore);

        spy.init();
        TimeUnit.SECONDS.sleep(1);
        verify(spy, never()).startUpdate(any(UpdateData.class));
        verify(updaterService, times(1)).getLast();
        verify(transactionVerifier, times(1)).process(any(Transaction.class));
        verify(updaterMediatorInstance, times(1)).addUpdateListener(any(Listener.class));
    }

    @Test
    public void testInitNullUpdateTransaction() throws Exception {
        UpdaterCore updaterCore = new UpdaterCoreImpl(updaterService, updaterMediatorInstance, updaterFactory, transactionVerifier);
        UpdaterCore spy = spy(updaterCore);

        spy.init();

        verify(spy, never()).startUpdate(any(UpdateData.class));
        verify(updaterService, times(1)).getLast();
        verify(transactionVerifier, never()).process(any(Transaction.class));
        verify(updaterMediatorInstance, times(1)).addUpdateListener(any(Listener.class));
    }


    @Test
    public void testInitNotUpdatedNullUpdateData() throws Exception {
        Transaction mockTransaction = new SimpleTransaction(1L, Update.MINOR);
        UpdateTransaction updateTransaction = new UpdateTransaction(mockTransaction, false);
        when(updaterService.getLast()).thenReturn(updateTransaction);
        UpdaterCore updaterCore = new UpdaterCoreImpl(updaterService, updaterMediatorInstance, updaterFactory, transactionVerifier);
        UpdaterCore spy = spy(updaterCore);

        spy.init();

        verify(spy, never()).startUpdate(any(UpdateData.class));
        verify(updaterService, times(1)).getLast();
        verify(updaterService, times(1)).clear();
        verify(transactionVerifier, times(1)).process(any(Transaction.class));
        verify(updaterMediatorInstance, times(1)).addUpdateListener(any(Listener.class));
    }

    @Test
    public void testInitUpdatedCriticalUpdateGreaterUpdateVersion() throws Exception {
        SimpleTransaction mockTransaction = new SimpleTransaction(1L, Update.CRITICAL);
        mockTransaction.setAttachment(attachment);
        UpdateTransaction updateTransaction = new UpdateTransaction(mockTransaction, true);
        when(updaterService.getLast()).thenReturn(updateTransaction);
        UpdaterCore updaterCore = new UpdaterCoreImpl(updaterService, updaterMediatorInstance, updaterFactory, transactionVerifier);
        UpdaterCore spy = spy(updaterCore);
        when(updaterMediatorInstance.getWalletVersion()).thenReturn(new Version("1.0.7"));

        spy.init();

        verify(spy, never()).startUpdate(any(UpdateData.class));
        verify(updaterService, times(1)).getLast();
        verify(updaterMediatorInstance, times(1)).suspendBlockchain();
        verify(updaterMediatorInstance, never()).addUpdateListener(any(Listener.class));
        UpdateInfo updateInfo = spy.getUpdateInfo();
        Assert.assertEquals(UpdateInfo.UpdateState.REQUIRED_MANUAL_INSTALL, updateInfo.getUpdateState());
        Assert.assertEquals(Level.CRITICAL, updateInfo.getLevel());
    }

    @Test
    public void testInitUpdatedNonCriticalUpdateGreaterUpdateVersion() throws Exception {
        SimpleTransaction mockTransaction = new SimpleTransaction(1L, Update.IMPORTANT);
        mockTransaction.setAttachment(attachment);
        UpdateTransaction updateTransaction = new UpdateTransaction(mockTransaction, true);
        when(updaterService.getLast()).thenReturn(updateTransaction);
        UpdaterCore updaterCore = new UpdaterCoreImpl(updaterService, updaterMediatorInstance, updaterFactory, transactionVerifier);
        UpdaterCore spy = spy(updaterCore);
        when(updaterMediatorInstance.getWalletVersion()).thenReturn(new Version("1.0.7"));

        spy.init();

        verify(updaterService, times(1)).getLast();
        verify(updaterMediatorInstance, times(1)).addUpdateListener(any(Listener.class));
        verify(updaterMediatorInstance, never()).suspendBlockchain();
    }

    @Test
    public void testInitUpdatedAllUpdatesLesserOrEqualUpdateVersion() throws Exception {
        SimpleTransaction mockTransaction = new SimpleTransaction(1L, Update.IMPORTANT);
        mockTransaction.setAttachment(attachment);
        UpdateTransaction updateTransaction = new UpdateTransaction(mockTransaction, true);
        when(updaterService.getLast()).thenReturn(updateTransaction);
        UpdaterCore updaterCore = new UpdaterCoreImpl(updaterService, updaterMediatorInstance, updaterFactory, transactionVerifier);
        UpdaterCore spy = spy(updaterCore);
        when(updaterMediatorInstance.getWalletVersion()).thenReturn(attachment.getAppVersion());

        spy.init();

        verify(updaterService, times(1)).getLast();
        verify(updaterMediatorInstance, times(1)).addUpdateListener(any(Listener.class));
        verify(spy, never()).startUpdate(any(UpdateData.class));
        verify(updaterMediatorInstance, never()).suspendBlockchain();
    }

    //    UpdaterCoreImpl startAvailableUpdate
    @Test
    public void testStartMinorUpdate() throws InterruptedException {
        SimpleTransaction mockTransaction = new SimpleTransaction(3L, Update.MINOR);
        mockTransaction.setAttachment(attachment);
        UpdateData updateData = new UpdateData(mockTransaction, decryptedUrl);
        UpdaterCore updaterCore = new UpdaterCoreImpl(updaterService, updaterMediatorInstance,
                transactionVerifier);
        doReturn(new UpdateTransaction(mockTransaction, false)).when(updaterService).getLast();
        doReturn(updateData).when(transactionVerifier).process(mockTransaction);
        UpdaterCore spy = spy(updaterCore);

        boolean started = spy.startAvailableUpdate();

        Assert.assertTrue(started);
        TimeUnit.SECONDS.sleep(1);
        Mockito.verify(updaterMediatorInstance, times(1)).suspendBlockchain();
        Mockito.verify(updaterMediatorInstance, times(1)).resumeBlockchain();
        UpdateInfo updateInfo = updaterCore.getUpdateInfo();
        Assert.assertEquals(updateInfo.getLevel(), Level.MINOR);
        Assert.assertEquals(updateInfo.getUpdateState(), UpdateInfo.UpdateState.FAILED_REQUIRED_START);

    }

    private List<Transaction> getRandomTransactions(int numberOfTransactions) {
        List<Transaction> transactions = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < numberOfTransactions; i++) {
            Transaction transaction = new SimpleTransaction(random.nextLong(), TransactionType.findTransactionType((byte) random.nextInt(8),
                    (byte) 0));
            transactions.add(transaction);
        }
        return transactions;
    }

}

//    @Test
//    public void testTriggerCriticalUpdate() throws Exception {
//        //Prepare testdata
//        Version testVersion = Version.from("1.0.8");
//        UpdaterCore.UpdateDataHolder holder = new UpdaterCore.UpdateDataHolder(new UpdateTransaction(TransactionType.Update.CRITICAL, 0L, 0L, TestUtil.atm(1L), 0L, 9, attachment), decryptedUrl);
//
//        //Mock dependent classes
//
//        Whitebox.setInternalState(updaterMediatorInstance, "updateInfo", UpdateInfo.getInstance());
//
//        //mock external methods
//        when(updaterMediatorInstance, "getBlockchainHeight").thenReturn(10);
//        doCallRealMethod().when(updaterMediatorInstance).setUpdateData(true, 10, holder.getTransaction().getHeight(), Level.CRITICAL, testVersion);
//        doCallRealMethod().when(updaterMediatorInstance).setUpdateState(any(UpdateInfo.UpdateState.class));
//
//        //spy target class
//        UpdaterCore updaterCore = spy(UpdaterCore.getInstance());
//
//        //mock inner private methods
//        doReturn(true).when(updaterCore, "tryUpdate", attachment, decryptedUrl, true);
//
//        //call target method
//        Whitebox.invokeMethod(updaterCore, "triggerUpdate", holder);
//
//        //verify methods invocations
//        verifyPrivate(updaterCore).invoke("tryUpdate", attachment, decryptedUrl, true);
//        UpdateInfo info = UpdateInfo.getInstance();
//        Assert.assertEquals(testVersion, info.getVersion());
//        Assert.assertEquals(9, info.getReceivedHeight());
//        Assert.assertEquals(10, info.getEstimatedHeight());
//        Assert.assertEquals(Level.CRITICAL, info.getLevel());
//        Assert.assertEquals(UpdateInfo.UpdateState.FINISHED, info.getUpdateState());
//
//    }
//
//    @Test
//    public void testBadTriggerCriticalUpdate() throws Exception {
//        //Prepare testdata
//        Version testVersion = Version.from("1.0.8");
//        UpdaterCore.UpdateDataHolder holder = new UpdaterCore.UpdateDataHolder(new UpdateTransaction(TransactionType.Update.CRITICAL, 0L, 0L, TestUtil.atm(1L), 0L, 9, attachment), decryptedUrl);
//
//        //Mock dependent classes
//
//        Whitebox.setInternalState(updaterMediatorInstance, "updateInfo", UpdateInfo.getInstance());
//
//        //mock external methods
//        doReturn(10).when(updaterMediatorInstance, "getBlockchainHeight");
//        doCallRealMethod().when(updaterMediatorInstance).setUpdateData(true, 10, holder.getTransaction().getHeight(), Level.CRITICAL, testVersion);
//        doCallRealMethod().when(updaterMediatorInstance).setUpdateState(any(UpdateInfo.UpdateState.class));
//
//        //spy target class
//        UpdaterCore updaterCore = spy(UpdaterCore.getInstance());
//
//        //mock inner private methods
//        doReturn(false).when(updaterCore, "tryUpdate", attachment, decryptedUrl, true);
//
//        //call target method
//        Whitebox.invokeMethod(updaterCore, "triggerUpdate", holder);
//
//        //verify methods invocations
//        verifyPrivate(updaterCore).invoke("tryUpdate", attachment, decryptedUrl, true);
//        UpdateInfo info = UpdateInfo.getInstance();
//        Assert.assertEquals(testVersion, info.getVersion());
//        Assert.assertEquals(9, info.getReceivedHeight());
//        Assert.assertEquals(10, info.getEstimatedHeight());
//        Assert.assertEquals(Level.CRITICAL, info.getLevel());
//        Assert.assertEquals(UpdateInfo.UpdateState.REQUIRED_MANUAL_INSTALL, info.getUpdateState());
//
//    }
//
//
//    @Test
//    public void testTriggerImportantUpdate() throws Exception {
//        //Prepare testdata
//        Version testVersion = Version.from("1.0.8");
//        UpdaterCore.UpdateDataHolder holder = new UpdaterCore.UpdateDataHolder(new UpdateTransaction(TransactionType.Update.IMPORTANT, 0L, 0L, TestUtil.atm(1L), 0L, 100, attachment), decryptedUrl);
//
//        //Mock dependent classes
//
//        Whitebox.setInternalState(updaterMediatorInstance, "updateInfo", UpdateInfo.getInstance());
//
//        //mock external methods
//        doCallRealMethod().when(updaterMediatorInstance).setUpdateData(anyBoolean(), anyInt(), anyInt(), any(Level.class), any(Version.class));
//        doCallRealMethod().when(updaterMediatorInstance).setUpdateState(any(UpdateInfo.UpdateState.class));
//
//        //spy target class
//        UpdaterCore updaterCore = spy(UpdaterCore.getInstance());
//
//        //mock inner private methods
//        doReturn(true).when(updaterCore, "scheduleUpdate", 101, attachment, decryptedUrl);
//        doReturn(101).when(updaterCore, "getUpdateHeightFromType", TransactionType.Update.IMPORTANT);
//
//        //call target method
//        Whitebox.invokeMethod(updaterCore, "triggerUpdate", holder);
//
//        //verify methods invocations
//        verifyPrivate(updaterCore).invoke("scheduleUpdate", anyInt(), any(Attachment.ImportantUpdate.class), anyString());
//        UpdateInfo info = UpdateInfo.getInstance();
//        Assert.assertEquals(testVersion, info.getVersion());
//        Assert.assertEquals(100, info.getReceivedHeight());
//        Assert.assertEquals(101, info.getEstimatedHeight());
//        Assert.assertEquals(Level.IMPORTANT, info.getLevel());
//        Assert.assertEquals(UpdateInfo.UpdateState.FINISHED, info.getUpdateState());
//
//    }
//
//
//    @Test
//    public void testTriggerMinorUpdate() throws Exception {
//        //Prepare testdata
//        Version testVersion = Version.from("1.0.8");
//        UpdaterCore.UpdateDataHolder holder = new UpdaterCore.UpdateDataHolder(new UpdateTransaction(TransactionType.Update.MINOR, 0L, 0L, TestUtil.atm(1L), 0L, 100, attachment), decryptedUrl);
//
//        //Mock dependent classes
//
//        Whitebox.setInternalState(updaterMediatorInstance, "updateInfo", UpdateInfo.getInstance());
//
//        //mock external methods
//        doCallRealMethod().when(updaterMediatorInstance).setUpdateData(anyBoolean(), anyInt(), anyInt(), any(Level.class), any(Version.class));
//        doCallRealMethod().when(updaterMediatorInstance).setUpdateState(any(UpdateInfo.UpdateState.class));
//
//        //spy target class
//        UpdaterCore updaterCore = spy(UpdaterCore.getInstance());
//
//        //mock inner private methods
//        doReturn(true).when(updaterCore, "scheduleUpdate", 101, attachment, decryptedUrl);
//        doReturn(101).when(updaterCore, "getUpdateHeightFromType", TransactionType.Update.MINOR);
//
//        //call target method
//        Whitebox.invokeMethod(updaterCore, "triggerUpdate", holder);
//
//        //verify
//        UpdateInfo info = UpdateInfo.getInstance();
//        Assert.assertEquals(testVersion, info.getVersion());
//        Assert.assertEquals(100, info.getReceivedHeight());
//        Assert.assertEquals(101, info.getEstimatedHeight());
//        Assert.assertEquals(Level.MINOR, info.getLevel());
//        Assert.assertEquals(UpdateInfo.UpdateState.REQUIRED_START, info.getUpdateState());
//
//    }
//
//
//    @Test
//    public void testVerifyJar() throws Exception {
//        Path signedJar = Files.createTempFile("test-verifyjar", ".jar");
//        try {
//            Set<Certificate> certificates = new HashSet<>();
//            certificates.add(UpdaterUtil.readCertificate("certs/1_1.crt"));
//            certificates.add(UpdaterUtil.readCertificate("certs/1_2.crt"));
//            certificates.add(UpdaterUtil.readCertificate("certs/2_1.crt"));
//            certificates.add(UpdaterUtil.readCertificate("certs/2_2.crt"));
//            PrivateKey privateKey = RSAUtil.getPrivateKey("certs/2_2.key");
//            JarGenerator generator = new JarGenerator(Files.newOutputStream(signedJar), UpdaterUtil.readCertificate("certs/2_2.crt"), privateKey);
//            generator.generate();
//            generator.close();
//
//            mockStatic(UpdaterUtil.class);
//            when(UpdaterUtil.readCertificates(CERTIFICATE_DIRECTORY, CERTIFICATE_SUFFIX, FIRST_DECRYPTION_CERTIFICATE_PREFIX, SECOND_DECRYPTION_CERTIFICATE_PREFIX)).thenReturn(certificates);
//
//            Object result = Whitebox.invokeMethod(UpdaterCore.getInstance(), "verifyJar", signedJar);
//            Assert.assertTrue(Boolean.parseBoolean(result.toString()));
//        }
//        finally {
//            Files.deleteIfExists(signedJar);
//        }
//    }
//
//    @Test
//    public void testProcessTransactions() throws Exception {
//        doReturn(true).when(fakeCheckerInstance, "verifyCertificates", CERTIFICATE_DIRECTORY);
//        mockStatic(UpdaterDbRepository.class);
//        Version testVersion = Version.from("1.0.7");
//        when(updaterMediatorInstance, "getWalletVersion").thenReturn(testVersion);
//
//        Platform currentPlatform = Platform.current();
//        Architecture currentArchitecture = Architecture.current();
//        Attachment.UpdateAttachment attachment = Attachment.UpdateAttachment.getAttachment(
//                currentPlatform,
//                currentArchitecture,
//                new DoubleByteArrayTuple(new byte[0], new byte[0]),
//                Version.from("1.0.8"),
//                new byte[0],
//                (byte) 0);
//
//        mockStatic(RSAUtil.class);
//        String fakeWalletUrl = "http://apollocurrency/ApolloWallet-" + testVersion + ".jar";
//        doReturn(fakeWalletUrl).when(RSAUtil.class, "tryDecryptUrl", CERTIFICATE_DIRECTORY, attachment.getUrl(), attachment
//                .getAppVersion());
//        Thread mock = mock(Thread.class);
//        doNothing().when(mock, "start");
//        whenNew(Thread.class).withAnyArguments().thenReturn(mock);
//        List<Transaction> randomTransactions = getRandomTransactions(6);
//        UpdateTransaction updateTransaction = new UpdateTransaction(TransactionType.Update.CRITICAL, 0, 0, TestUtil.atm
//                (1L), 0, attachment);
//        randomTransactions.add(updateTransaction);
//
//        when(updaterMediatorInstance, "isUpdateTransaction", updateTransaction).thenReturn(true);
//        Whitebox.invokeMethod(UpdaterCore.getInstance(), "processTransactions", randomTransactions);
//        Mockito.verify(mock, Mockito.times(1)).start();
//        UpdaterCore.UpdateDataHolder updateDataHolder = Whitebox.getInternalState(UpdaterCore.getInstance(), "updateDataHolder");
//        Assert.assertEquals(updateTransaction, updateDataHolder.getTransaction());
//        Assert.assertEquals(fakeWalletUrl, updateDataHolder.getDecryptedUrl());
//    }
//
//    private List<Transaction> getRandomTransactions(int numberOfTransactions) {
//        List<Transaction> transactions = new ArrayList<>();
//        Random random = new Random();
//        for (int i = 0; i < numberOfTransactions; i++) {
//            Transaction transaction = new SimpleTransactionImpl(TransactionType.findTransactionType((byte) random.nextInt(8), (byte) 0), random.nextLong(), random.nextLong(), TestUtil.atm(random.nextInt(10_000)), TestUtil.atm(random.nextInt(10_000)));
//            transactions.add(transaction);
//        }
//        return transactions;
//    }
//
//    @Test
//    public void testTryUpdate() throws Exception {
//        //Prepare testdata
//        Attachment.UpdateAttachment attachment = Attachment.UpdateAttachment.getAttachment(
//                Platform.current(),
//                Architecture.current(),
//                new DoubleByteArrayTuple(new byte[0], new byte[0]),
//                Version.from("1.0.8"),
//                new byte[0],
//                (byte) 0);
//        Path fakeJarPath = Paths.get("");
//        Path fakeUnpackedDirPath = Paths.get("");
//
//        //mock external methods
//        doReturn(fakeJarPath).when(fakeDownloaderInstance, "tryDownload", decryptedUrl, attachment.getHash());
//        doReturn(fakeUnpackedDirPath).when(fakeJarUnpackerInstance, "unpack", fakeJarPath);
//        doNothing().when(fakePlatformDependentUpdaterInstance, "start", fakeUnpackedDirPath, attachment.getPlatform());
//
//        //spy target class
//        UpdaterCore updaterCore = spy(UpdaterCore.getInstance());
//
//        //mock inner private methods
//        doReturn(true).when(updaterCore, "verifyJar", fakeJarPath);
//        doNothing().when(updaterCore, "stopForgingAndBlockAcceptance");
//
//
//        //call target method
//        Object result = Whitebox.invokeMethod(updaterCore, "tryUpdate", attachment, decryptedUrl);
//        Assert.assertTrue((Boolean) result);
//
//        //verify methods invocations
//        verifyPrivate(updaterCore).invoke("verifyJar", fakeJarPath);
//        Mockito.verify(fakePlatformDependentUpdaterInstance, Mockito.times(1)).continueUpdate(fakeUnpackedDirPath, attachment.getPlatform());
//    }
//
//    @Test
//    public void testTryUpdateWithWaiting() throws Exception {
//        //Prepare testdata
//
//        Path fakeJarPath = Paths.get("");
//        Path fakeUnpackedDirPath = Paths.get("");
//
//        //mock external methods
//        doReturn(fakeJarPath).when(fakeDownloaderInstance, "tryDownload", decryptedUrl, attachment.getHash());
//        doReturn(fakeUnpackedDirPath).when(fakeJarUnpackerInstance, "unpack", fakeJarPath);
//        doNothing().when(fakePlatformDependentUpdaterInstance, "start", fakeUnpackedDirPath, attachment.getPlatform());
//
//        //spy target class
//        UpdaterCore updaterCore = spy(UpdaterCore.getInstance());
//
//        //mock inner private methods
//        doReturn(true).when(updaterCore, "verifyJar", fakeJarPath);
//        doNothing().when(updaterCore, "stopForgingAndBlockAcceptance");
//        when(updaterMediatorInstance.getBlockchainHeight()).then(new Answer<Integer>() {
//            private int counter = 10;
//
//            @Override
//            public Integer answer(InvocationOnMock invocation) throws Throwable {
//                return counter++;
//            }
//        });
//        //call target method
//        Object result = Whitebox.invokeMethod(updaterCore, "tryUpdate", attachment, decryptedUrl, true);
//        Assert.assertTrue((Boolean) result);
//
//        //verify methods invocations
//        verifyPrivate(updaterCore).invoke("verifyJar", fakeJarPath);
//        Mockito.verify(fakePlatformDependentUpdaterInstance, Mockito.times(1)).continueUpdate(fakeUnpackedDirPath, attachment.getPlatform());
//        Mockito.verify(updaterMediatorInstance, Mockito.times(4)).getBlockchainHeight();
//    }
//
//    @Test
//    public void testWaitBlocks() throws Exception {
//        WalletRunner runner = new WalletRunner();
//        try {
//            runner.run();
//            TimeUnit.SECONDS.sleep(5);
//            when(updaterMediatorInstance.getBlockchainHeight()).thenCallRealMethod();
//            int startHeight = UpdaterMediator.getInstance().getBlockchainHeight();
//            long startTime = System.currentTimeMillis() / 1000;
//            Whitebox.invokeMethod(UpdaterCore.getInstance(), "waitBlocks", 1, 70);
//            long finishTime = System.currentTimeMillis() / 1000;
//            int finishHeight = UpdaterMediator.getInstance().getBlockchainHeight();
//            if (finishTime - startTime < 70 && finishHeight - startHeight < 1) {
//                Assert.fail("Bad waiting");
//            }
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            runner.shutdown();
//        }
//    }
