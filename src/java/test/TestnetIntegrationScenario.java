package test;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashMap;
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
        int peerQuantity = URLS.size() - 1;
        int peers = 0;
        if (CLIENT.getPeersCount(TEST_LOCALHOST) != peerQuantity + 1) {
            return false;
        }
        for (String ip : URLS) {
            peers = CLIENT.getPeersCount(ip);
            if (peers != peerQuantity) {
                return false;
            }
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
}
