package apl.http;/*
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

import apl.NodeClient;
import apl.TestData;
import apl.TestUtil;
import dto.ForgingDetails;
import dto.Transaction;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public class TestnetIntegrationScenario {
    private static final Map<String, String> ACCOUNTS = new HashMap<>(TestUtil.loadKeys(TestData.TEST_FILE));
    private static final NodeClient CLIENT = new NodeClient();
    private static final Logger LOG = getLogger(TestnetIntegrationScenario.class);
    private static final Random RANDOM = new Random();
    private static String adminPass;

    static {
        Properties properties = new Properties();
        try {
            properties.load(Files.newInputStream(Paths.get("conf/apl.properties")));
            adminPass = String.valueOf(properties.get("apl.adminPassword"));
        }
        catch (IOException e) {
            LOG.error("Cannot read apl.properties file", e);
        }
    }
    @Test
    public void testSendTransaction() throws Exception {
        testIsFork();
        testIsAllPeersConnected();
        Transaction transaction = sendRandomTransaction();
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
        Transaction transaction = sendRandomPrivateTransaction();
        LOG.info("Private {} was sent. Wait for confirmation", transaction);
        Assert.assertTrue(waitForConfirmation(transaction, 600));
        LOG.info("Private {} was successfully confirmed. Checking peers...", transaction);
        testIsAllPeersConnected();
        testIsFork();
    }

    @Test
    public void testStopForgingAndBlockAcceptance() throws Exception {
        testIsFork();
        testIsAllPeersConnected();
        LOG.info("Starting forging on {} accounts", ACCOUNTS.size());
        ACCOUNTS.forEach((accountRS, secretPhrase) -> {
            try {
                CLIENT.startForging(TestData.TEST_LOCALHOST, secretPhrase);
            }
            catch (IOException e) {
                LOG.error("Cannot start forging for account: " + accountRS + " on " + TestData.TEST_LOCALHOST, e);
            }
        });
//        LOG.info("Waiting 2 blocks creation...");
//        waitBlocks(2);
        TimeUnit.SECONDS.sleep(5);
        LOG.info("Verifying forgers on localhost");
        List<ForgingDetails> forgers = CLIENT.getForging(TestData.TEST_LOCALHOST, null, adminPass);
        Assert.assertEquals(5, forgers.size());
        forgers.forEach( generator -> {
            if (!ACCOUNTS.containsKey(generator.getAccountRS())) {
                Assert.fail("Incorrect generator: " + generator.getAccountRS());
            }
        });
//        UpdaterUtil.stopForgingAndBlockAcceptance();
        LOG.info("Stopping forging and peer server...");
        long remoteHeight = CLIENT.getBlockchainHeight(TestUtil.randomUrl());
        CLIENT.stopForgingAndBlockAcceptance(TestData.TEST_LOCALHOST, adminPass);
        long localHeight = CLIENT.getBlockchainHeight(TestData.TEST_LOCALHOST);
        LOG.info("Local height / Remote height: {}/{}", localHeight, remoteHeight);
        Assert.assertEquals(localHeight, remoteHeight);
        Assert.assertEquals(CLIENT.getBlock(TestUtil.randomUrl(), remoteHeight), CLIENT.getBlock(TestData.TEST_LOCALHOST, localHeight));
        LOG.info("Checking forgers on node (Assuming no forgers)");
        forgers = CLIENT.getForging(TestData.TEST_LOCALHOST, null, adminPass);
        Assert.assertEquals(0, forgers.size());
        LOG.info("Waiting 5 blocks creation...");
        waitBlocks(5);
        remoteHeight = CLIENT.getBlockchainHeight(TestUtil.randomUrl());
        long actualLocalHeight = CLIENT.getBlockchainHeight(TestData.TEST_LOCALHOST);
        LOG.info("Comparing blockchain height local/remote: {}/{}", actualLocalHeight, remoteHeight);
        Assert.assertEquals(localHeight, actualLocalHeight);
        Assert.assertEquals(remoteHeight, localHeight + 5);
        testIsFork();
        testIsAllPeersConnected();
    }

    private boolean waitForConfirmation(Transaction transaction, int seconds) throws InterruptedException, IOException {
        while (seconds > 0) {
            seconds -= 1;
            Transaction receivedTransaction = CLIENT.getTransaction(TestUtil.randomUrl(), transaction.getFullHash());
            if (receivedTransaction.getConfirmations() != null && receivedTransaction.getConfirmations() > 0) {
                return true;
            }
            TimeUnit.SECONDS.sleep(1);
        }
        return false;
    }


    @Test
    public void test() throws Exception {
        System.out.println("PeerCount=" + CLIENT.getPeersCount(TestData.URLS.get(0)));
        System.out.println("BLOCKS=" + CLIENT.getBlocksList(TestData.URLS.get(0), false, null));
        System.out.println("Blockchain height: " + CLIENT.getBlockchainHeight(TestData.URLS.get(0)));
        System.out.println("Forgers="+ CLIENT.getForging(TestData.TEST_LOCALHOST, null, adminPass));
    }

    @Test
    public void testIsFork() throws Exception {
        Assert.assertFalse("Fork was occurred!",isFork());
        LOG.info("Fork is not detected. Current height {}", CLIENT.getBlockchainHeight(TestUtil.randomUrl()));
    }
    @Test
    public void testIsAllPeersConnected() throws Exception {
        Assert.assertTrue("All peers are NOT connected!", isAllPeersConnected());
        LOG.info("All peers are connected. Total peers: {}", CLIENT.getPeersCount(TestUtil.randomUrl()));
    }


    private boolean isFork() throws Exception {
        Long currentBlockchainHeight;
        Long prevBlockchainHeight = null;
        for (String url : TestData.URLS) {
            currentBlockchainHeight = CLIENT.getBlockchainHeight(url);
            if (prevBlockchainHeight != null && !currentBlockchainHeight.equals(prevBlockchainHeight)) {
                return true;
            }
            prevBlockchainHeight = currentBlockchainHeight;
        }
        return false;
    }

    private boolean isAllPeersConnected() throws Exception {
        int peerQuantity = (int) Math.ceil((double) TestData.URLS.size() * 0.51);
        int maxPeerQuantity = TestData.URLS.size();
        int peers = 0;
        int localHostPeers = CLIENT.getPeersCount(TestData.TEST_LOCALHOST);
        if (localHostPeers < peerQuantity) {
            LOG.error("Localhost peer has {}/{} peers. Required >= {}", localHostPeers, maxPeerQuantity, peerQuantity);
            return false;
        }
        for (String ip : TestData.URLS) {
            peers = CLIENT.getPeersCount(ip);
            if (peers < peerQuantity) {
                LOG.error("Peer with {} has {}/{} peers. Required >= {}", ip, peers, maxPeerQuantity, peerQuantity);
                return false;
            }
            LOG.info("Peer with {} has {}/{} peers.", ip, peers, maxPeerQuantity);
        }
        return true;
    }

    private Transaction sendRandomTransaction() throws Exception {
        String host = TestUtil.randomUrl();
        String sender = TestUtil.getRandomRS(ACCOUNTS);
        String recipient = TestUtil.getRandomRecipientRS(ACCOUNTS, sender);
        String secretPhrase = ACCOUNTS.get(sender);
        Long amount = TestUtil.atm(RANDOM.nextInt(10) + 1);
        return CLIENT.sendMoneyTransaction(host, secretPhrase, recipient, amount);
    }

    private Transaction sendRandomPrivateTransaction() throws Exception {
        String host = TestUtil.randomUrl();
        String sender = TestUtil.getRandomRS(ACCOUNTS);
        String recipient = TestUtil.getRandomRecipientRS(ACCOUNTS, sender);
        String secretPhrase = ACCOUNTS.get(sender);
        Long amount = TestUtil.atm(RANDOM.nextInt(10) + 1);
        return CLIENT.sendMoneyPrivateTransaction(host, secretPhrase, recipient, amount, NodeClient.DEFAULT_FEE, NodeClient.DEFAULT_DEADLINE);
    }

    private void waitBlocks(long numberOfBlocks) throws Exception {
        long startBlockchainHeight = CLIENT.getBlockchainHeight(TestUtil.randomUrl());
        long currentBlockchainHeight = CLIENT.getBlockchainHeight(TestUtil.randomUrl());
        while (currentBlockchainHeight != numberOfBlocks + startBlockchainHeight) {
            TimeUnit.MILLISECONDS.sleep(300);
            currentBlockchainHeight = CLIENT.getBlockchainHeight(TestUtil.randomUrl());
        }
        TimeUnit.MILLISECONDS.sleep(300);
    }
}
