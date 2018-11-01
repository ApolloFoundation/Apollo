/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import static com.apollocurrency.aplwallet.apl.TestConstants.ADMIN_PASS;
import static com.apollocurrency.aplwallet.apl.TestConstants.TEST_LOCALHOST;
import static org.slf4j.LoggerFactory.getLogger;
import static util.TestUtil.atm;

import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.NodeClient;
import com.apollocurrency.aplwallet.apl.TestConstants;
import com.apollocurrency.aplwallet.apl.Transaction;
import com.apollocurrency.aplwallet.apl.TransactionType;
import com.apollocurrency.aplwallet.apl.Version;
import com.apollocurrency.aplwallet.apl.updater.Architecture;
import com.apollocurrency.aplwallet.apl.updater.DoubleByteArrayTuple;
import com.apollocurrency.aplwallet.apl.updater.Platform;
import com.apollocurrency.aplwallet.apl.util.Convert;
import dto.Block;
import dto.ForgingDetails;
import dto.JSONTransaction;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import util.TestUtil;
import util.WalletRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
@Ignore
public class TestnetIntegrationScenario {
    private static final Map<String, String> ACCOUNTS = new HashMap<>(TestUtil.loadKeys(TestConstants.TEST_FILE));
    private static final NodeClient CLIENT = new NodeClient();
    private static final Logger LOG = getLogger(TestnetIntegrationScenario.class);
    private static final Random RANDOM = new Random();
    private volatile WalletRunner runner;

    @After
    public  void tearDown() throws Exception {
        runner.shutdown();
    }

    @Before
    public void setUp() throws Exception {
        runner = new WalletRunner();
        runner.run();
        //wait for init
        TimeUnit.SECONDS.sleep(5);
    }

    @Test
    public void testSendTransaction() throws Exception {
        testIsFork();
        testIsAllPeersConnected();
        JSONTransaction transaction = sendRandomTransaction();
        LOG.info("Ordinary {} was sent. Wait for confirmation", transaction);
        Assert.assertTrue(waitForConfirmation(transaction, 600));
        LOG.info("Ordinary {} was successfully confirmed. Checking peers...", transaction);
        testIsAllPeersConnected();
        testIsFork();
    }

    @Test
    public void testSendPrivateTransaction() throws Exception {
        testIsFork();
        testIsAllPeersConnected();
        JSONTransaction transaction = sendRandomPrivateTransaction();
        LOG.info("Private {} was sent. Wait for confirmation", transaction);
        Assert.assertTrue(waitForConfirmation(transaction, 600));
        LOG.info("Private {} was successfully confirmed. Checking peers...", transaction);
        testIsAllPeersConnected();
        testIsFork();
    }

    @Test
    @Ignore
    public void testSendCriticalUpdate() throws Exception {
        testIsFork();
        testIsAllPeersConnected();
        String nodeApiUrl = TestUtil.randomUrl(runner.getUrls());
        String secretPhrase = ACCOUNTS.get(TestUtil.getRandomRS(ACCOUNTS));
        long feeATM = atm(1L);
        DoubleByteArrayTuple updateUrl = new DoubleByteArrayTuple(new byte[0], new byte[0]);
        Assert.fail("Encrypted url is required");
        Version peerWalletVersion = CLIENT.getRemoteVersion(randomUrl());
        Version newWalletVersion = peerWalletVersion.incrementVersion();
        String hashString = "a2c1e47afd4b25035a025091ec3c33ec1992d09e7f3c05875d79e660139220a4";
        byte[] hash = Convert.parseHexString(hashString);
        Transaction updateTransaction = CLIENT.sendUpdateTransaction(nodeApiUrl, secretPhrase, feeATM, 0, updateUrl, newWalletVersion, Architecture.AMD64, Platform.LINUX, hashString, 5);
        Attachment.UpdateAttachment attachment = Attachment.UpdateAttachment.getAttachment(Platform.LINUX, Architecture.AMD64, updateUrl,
                newWalletVersion,
                hash, (byte) 0);
        Assert.assertEquals(TransactionType.Update.CRITICAL, updateTransaction.getType());
        Assert.assertEquals(attachment, updateTransaction.getAttachment());
        waitBlocks(1);
        waitFor((String url)-> {
                    try {
                        Version walletCurrentVersion = CLIENT.getRemoteVersion(url);
                        return (walletCurrentVersion.equals(newWalletVersion));
                    }
                    catch (IOException e) {
                        return false;
                    }
                }
            , 600
        );
        waitBlocks(1);
        testIsFork();
        testIsAllPeersConnected();
    }

    private void waitFor(Predicate<String> condition, int seconds) throws InterruptedException {
        double totalSeconds = 0;
        while (!condition.test(randomUrl())) {
            if (totalSeconds >= seconds) {
                throw new RuntimeException("Time out");
            }
            TimeUnit.MILLISECONDS.sleep(500);
            totalSeconds += 0.5;
        }
    }

    private String randomUrl() {
        return TestUtil.randomUrl(runner.getUrls());
    }

    @Test
    public void testStopForgingAndBlockAcceptance() throws Exception {
        runner.disableReloading();
        testIsFork();
        testIsAllPeersConnected();
        LOG.info("Starting forging on {} accounts", ACCOUNTS.size());
        ACCOUNTS.forEach((accountRS, secretPhrase) -> {
            try {
                CLIENT.startForging(TestConstants.TEST_LOCALHOST, secretPhrase);
            }
            catch (IOException e) {
                String errorMessage = "Cannot start forging for account: " + accountRS + " on " + TestConstants.TEST_LOCALHOST;
                LOG.error(errorMessage, e);
                Assert.fail(errorMessage);
            }
        });
        TimeUnit.SECONDS.sleep(5);
        LOG.info("Verifying forgers on localhost");
        List<ForgingDetails> forgers = CLIENT.getForging(TestConstants.TEST_LOCALHOST, null, ADMIN_PASS);
        Assert.assertEquals(5, forgers.size());
        forgers.forEach( generator -> {
            String accountRS = generator.getAccount().getAccountRS();
            if (!ACCOUNTS.containsKey(accountRS)) {
                Assert.fail("Incorrect generator: " + accountRS);
            }
        });
        LOG.info("Stopping forging and peer server...");
        int remoteHeight = CLIENT.getBlockchainHeight(TestUtil.randomUrl(runner.getUrls()));
        Class updaterCore = runner.loadClass("com.apollocurrency.aplwallet.apl.updater.UpdaterCore");
        Whitebox.invokeMethod(updaterCore.getMethod("getInstance").invoke(null, null), "stopForgingAndBlockAcceptance");
        int localHeight = CLIENT.getBlockchainHeight(TestConstants.TEST_LOCALHOST);
        LOG.info("Local height / Remote height: {}/{}", localHeight, remoteHeight);
        Assert.assertEquals(localHeight, remoteHeight);
        Assert.assertEquals(CLIENT.getBlock(TestUtil.randomUrl(runner.getUrls()), remoteHeight), CLIENT.getBlock(TestConstants.TEST_LOCALHOST, localHeight));
        LOG.info("Checking forgers on node (Assuming 5 forgers)");
        forgers = CLIENT.getForging(TestConstants.TEST_LOCALHOST, null, ADMIN_PASS);
        Assert.assertEquals(5, forgers.size());
        LOG.info("Waiting 5 blocks creation...");
        waitBlocks(5);
        remoteHeight = CLIENT.getBlockchainHeight(TestUtil.randomUrl(runner.getUrls()));
        long actualLocalHeight = CLIENT.getBlockchainHeight(TestConstants.TEST_LOCALHOST);
        LOG.info("Comparing blockchain height local/remote: {}/{}", actualLocalHeight, remoteHeight);
        Assert.assertEquals(localHeight, actualLocalHeight);
        Assert.assertTrue(remoteHeight >= localHeight + 5);
        testIsFork();
        testIsAllPeersConnected();
    }

    private boolean waitForConfirmation(JSONTransaction transaction, int seconds) throws Exception {
        while (seconds > 0) {
            seconds -= 1;
            JSONTransaction receivedTransaction;
            if (transaction.getType() == TransactionType.Payment.PRIVATE) {
                receivedTransaction = CLIENT.getPrivateTransaction(TestUtil.randomUrl(runner.getUrls()),
                        ACCOUNTS.get(Convert.rsAccount(transaction.getSenderId())),
                        transaction.getFullHash(), null);
            } else {
                receivedTransaction = CLIENT.getTransaction(TestUtil.randomUrl(runner.getUrls()), transaction.getFullHash());
            }
            if (receivedTransaction != null && receivedTransaction.getNumberOfConfirmations() > 0) {
                return true;
            }
            TimeUnit.SECONDS.sleep(1);
        }
        return false;
    }


    @Test
    @Ignore
    public void test() throws Exception {
        System.out.println("PeerCount=" + CLIENT.getPeersCount(runner.getUrls().get(0)));
        System.out.println("BLOCKS=" + CLIENT.getBlocksList(runner.getUrls().get(0), false, null));
        System.out.println("Blockchain height: " + CLIENT.getBlockchainHeight(runner.getUrls().get(0)));
        System.out.println("Forgers="+ CLIENT.getForging(TestConstants.TEST_LOCALHOST, null, ADMIN_PASS));
    }

    @Test
    public void testIsFork() throws Exception {
        Assert.assertFalse("Fork was occurred!",isFork(3));
        LOG.info("Fork is not detected. Current height {}", CLIENT.getBlockchainHeight(TestUtil.randomUrl(runner.getUrls())));
    }
    @Test
    public void testIsAllPeersConnected() throws Exception {
        Assert.assertTrue("All peers are NOT connected!", isAllPeersConnected());
        LOG.info("All peers are connected. Total peers: {}", CLIENT.getPeersCount(TestUtil.randomUrl(runner.getUrls())));
    }


    private boolean isFork() throws Exception {
        int height = CLIENT.getBlockchainHeight(TEST_LOCALHOST);
        Set<Block> peersLastBlocks = new HashSet<>();
        for (String url : runner.getUrls()) {
            Block block = CLIENT.getBlock(url, height);
            peersLastBlocks.add(block);
            if (peersLastBlocks.size() > 1) {
                LOG.error("Peer " + url + " is on Fork! " + block.toString());
                return true;
            }
        }
        return false;
    }

    private boolean isFork(int numberOfChecks) throws Exception {
        for (int i = 0; i < numberOfChecks; i++) {
            if (isFork()) {
                LOG.debug("Check {}/{}. Fork is occurred", i+1, numberOfChecks);
            } else {
                return false;
            }
            TimeUnit.SECONDS.sleep(2);
        }
        return true;
    }

    private boolean isAllPeersConnected() throws Exception {
        int peerQuantity = (int) Math.ceil((double) runner.getUrls().size() * 0.51);
        int maxPeerQuantity = runner.getUrls().size();
        int peers = 0;
        int localHostPeers = CLIENT.getPeersCount(TestConstants.TEST_LOCALHOST);
        if (localHostPeers < peerQuantity) {
            LOG.error("Localhost peer has {}/{} peers. Required >= {}", localHostPeers, maxPeerQuantity, peerQuantity);
            return false;
        }
        for (String ip : runner.getUrls()) {
            peers = CLIENT.getPeersCount(ip);
            if (peers < peerQuantity) {
                LOG.error("Peer with {} has {}/{} peers. Required >= {}", ip, peers, maxPeerQuantity, peerQuantity);
                return false;
            }
            LOG.info("Peer with {} has {}/{} peers.", ip, peers, maxPeerQuantity);
        }
        return true;
    }

    private JSONTransaction sendRandomTransaction() throws Exception {
        String host = TestUtil.randomUrl(runner.getUrls());
        String sender = TestUtil.getRandomRS(ACCOUNTS);
        String recipient = TestUtil.getRandomRecipientRS(ACCOUNTS, sender);
        String secretPhrase = ACCOUNTS.get(sender);
        Long amount = TestUtil.atm(RANDOM.nextInt(10) + 1);
        return CLIENT.sendMoneyTransaction(host, secretPhrase, recipient, amount);
    }

    private JSONTransaction sendRandomPrivateTransaction() throws Exception {
        String host = TestUtil.randomUrl(runner.getUrls());
        String sender = TestUtil.getRandomRS(ACCOUNTS);
        String recipient = TestUtil.getRandomRecipientRS(ACCOUNTS, sender);
        String secretPhrase = ACCOUNTS.get(sender);
        Long amount = TestUtil.atm(RANDOM.nextInt(10) + 1);
        return CLIENT.sendMoneyPrivateTransaction(host, secretPhrase, recipient, amount, NodeClient.DEFAULT_FEE, NodeClient.DEFAULT_DEADLINE);
    }

    private void waitBlocks(long numberOfBlocks) throws Exception {
        List<String> urls = runner.getUrls();
        long startBlockchainHeight = CLIENT.getBlockchainHeight(TestUtil.randomUrl(urls));
        long currentBlockchainHeight = CLIENT.getBlockchainHeight(TestUtil.randomUrl(urls));
        while (currentBlockchainHeight != numberOfBlocks + startBlockchainHeight) {
            TimeUnit.MILLISECONDS.sleep(300);
            currentBlockchainHeight = CLIENT.getBlockchainHeight(TestUtil.randomUrl(urls));
        }
        TimeUnit.MILLISECONDS.sleep(300);
    }
}
