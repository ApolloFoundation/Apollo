package test;

import apl.Apl;
import apl.UpdaterUtil;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;
import static test.TestData.*;
import static test.TestUtil.*;

public class TestnetIntegrationScenario {
    private static final Map<String, String> ACCOUNTS = new HashMap<>(TestUtil.loadKeys(TEST_FILE));
    private static final NodeClient CLIENT = new NodeClient();
    private static final Logger LOG = getLogger(TestnetIntegrationScenario.class);
    private static final Random RANDOM = new Random();
    private static final String adminPass = Apl.getStringProperty("adminPassword");
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
        ACCOUNTS.forEach((accountRS, secretPhrase) -> {
            try {
                CLIENT.startForging(TEST_LOCALHOST, secretPhrase);
            }
            catch (IOException e) {
                LOG.error("Cannot start forging for account: " + accountRS + " on " + TEST_LOCALHOST, e);
            }
        });
        waitBlocks(2);
        List<ForgingDetails> forgers = CLIENT.getForging(TEST_LOCALHOST, null, adminPass);
        Assert.assertEquals(6, forgers.size());
        forgers.forEach( generator -> {
            if (!ACCOUNTS.containsKey(generator.getAccountRS())) {
                Assert.fail("Incorrect generator: " + generator.getAccountRS());
            }
        });
        waitBlocks(2);
        UpdaterUtil.stopForgingAndBlockAcceptance();
        long localHeight = CLIENT.getBlockchainHeight(TEST_LOCALHOST);
        long remoteHeight = CLIENT.getBlockchainHeight(randomUrl());
        Assert.assertEquals(localHeight, remoteHeight);
        Assert.assertEquals(CLIENT.getBlock(randomUrl(), remoteHeight), CLIENT.getBlock(TEST_LOCALHOST, localHeight));
        forgers = CLIENT.getForging(TEST_LOCALHOST, null, adminPass);
        Assert.assertEquals(0, forgers.size());
        waitBlocks(5);
        remoteHeight = CLIENT.getBlockchainHeight(randomUrl());
        Assert.assertEquals(localHeight, CLIENT.getBlockchainHeight(TEST_LOCALHOST).longValue());
        Assert.assertEquals(remoteHeight, localHeight + 5);
        testIsFork();
        testIsAllPeersConnected();
    }

    private boolean waitForConfirmation(Transaction transaction, int seconds) throws InterruptedException, IOException {
        while (seconds > 0) {
            seconds -= 1;
            Transaction receivedTransaction = CLIENT.getTransaction(randomUrl(), transaction.getFullHash());
            if (receivedTransaction.getConfirmations() != null && receivedTransaction.getConfirmations() > 0) {
                return true;
            }
            TimeUnit.SECONDS.sleep(1);
        }
        return false;
    }


    @Test
    public void test() throws Exception {
        System.out.println("PeerCount=" + CLIENT.getPeersCount(URLS.get(0)));
        System.out.println("BLOCKS=" + CLIENT.getBlocksList(URLS.get(0), false, null));
        System.out.println("Blockchain height: " + CLIENT.getBlockchainHeight(URLS.get(0)));
        System.out.println("Forgers="+ CLIENT.getForging(TEST_LOCALHOST, null, adminPass));
    }

    @Test
    public void testIsFork() throws Exception {
        Assert.assertFalse("Fork was occurred!",isFork());
        LOG.info("Fork is not detected. Current height {}", CLIENT.getBlockchainHeight(randomUrl()));
    }
    @Test
    public void testIsAllPeersConnected() throws Exception {
        Assert.assertTrue("All peers are NOT connected!", isAllPeersConnected());
        LOG.info("All peers are connected. Total peers: {}", CLIENT.getPeersCount(randomUrl()));
    }


    private boolean isFork() throws Exception {
        Long currentBlockchainHeight;
        Long prevBlockchainHeight = null;
        for (String url : URLS) {
            currentBlockchainHeight = CLIENT.getBlockchainHeight(url);
            if (prevBlockchainHeight != null && !currentBlockchainHeight.equals(prevBlockchainHeight)) {
                return true;
            }
            prevBlockchainHeight = currentBlockchainHeight;
        }
        return false;
    }

    private boolean isAllPeersConnected() throws Exception {
        int peerQuantity = (int) Math.ceil((double)URLS.size() * 0.51);
        int peers = 0;
        int localHostPeers = CLIENT.getPeersCount(TEST_LOCALHOST);
        if (localHostPeers >= peerQuantity) {
            LOG.error("Localhost peer has {}/{} peers.", localHostPeers, peerQuantity);
            return false;
        }
        for (String ip : URLS) {
            peers = CLIENT.getPeersCount(ip);
            if (peers >= peerQuantity) {
                LOG.error("Peer with {} has {}/{} peers.", ip, peers, peerQuantity);
                return false;
            }
            LOG.info("Peer with {} has {}/{} peers.", ip, peers, peerQuantity);
        }
        return true;
    }

    private Transaction sendRandomTransaction() throws Exception {
        String host = randomUrl();
        String sender = getRandomRS(ACCOUNTS);
        String recipient = getRandomRecipientRS(ACCOUNTS, sender);
        String secretPhrase = ACCOUNTS.get(sender);
        Long amount = nqt(RANDOM.nextInt(10) + 1);
        return CLIENT.sendMoneyTransaction(host, secretPhrase, recipient, amount);
    }

    private Transaction sendRandomPrivateTransaction() throws Exception {
        String host = randomUrl();
        String sender = getRandomRS(ACCOUNTS);
        String recipient = getRandomRecipientRS(ACCOUNTS, sender);
        String secretPhrase = ACCOUNTS.get(sender);
        Long amount = nqt(RANDOM.nextInt(10) + 1);
        return CLIENT.sendMoneyPrivateTransaction(host, secretPhrase, recipient, amount, NodeClient.DEFAULT_FEE, NodeClient.DEFAULT_DEADLINE);
    }

    private void waitBlocks(long numberOfBlocks) throws Exception {
        long startBlockchainHeight = CLIENT.getBlockchainHeight(randomUrl());
        long currentBlockchainHeight = CLIENT.getBlockchainHeight(randomUrl());
        while (currentBlockchainHeight != numberOfBlocks + startBlockchainHeight) {
            TimeUnit.MILLISECONDS.sleep(300);
            currentBlockchainHeight = CLIENT.getBlockchainHeight(randomUrl());
        }
        TimeUnit.MILLISECONDS.sleep(300);
    }
}
