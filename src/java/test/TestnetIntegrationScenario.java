package test;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;
import static test.TestData.*;
import static test.TestUtil.nqt;

public class TestnetIntegrationScenario {
    private static final Map<String, String> ACCOUNTS = new HashMap<>(TestUtil.loadKeys(TEST_FILE));
    private static final NodeClient CLIENT = new NodeClient();
    private static final Logger LOG = getLogger(TestnetIntegrationScenario.class);
    private static final Random RANDOM  = new Random();

    @Test
    public void testSendTransaction() throws Exception {
        Assert.assertFalse(isFork());
        LOG.info("Fork is not detected. Current height {}", CLIENT.getBlockchainHeight(randomUrl()));
        Assert.assertTrue(isAllPeersConnected());
        LOG.info("All peers are connected. Total peers: {}", CLIENT.getPeersCount(randomUrl()));
        Transaction transaction = sendRandomTransaction();
        LOG.info("{} was sent. Wait for confirmation", transaction);
        Assert.assertTrue(waitForConfirmation(transaction, 600));
        LOG.info("{} was successfully confirmed. Checking peers...",transaction);
        Assert.assertTrue(isAllPeersConnected());
        LOG.info("All peers are connected. Total peers: {}", CLIENT.getPeersCount(randomUrl()));
        Assert.assertFalse(isFork());
        LOG.info("Fork is not detected. Current height {}", CLIENT.getBlockchainHeight(randomUrl()));
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

    private String randomUrl() {
        return URLS.get(RANDOM.nextInt(URLS.size()));
    }

//    @Test
//    public void test() throws Exception {
//        System.out.println(CLIENT.getPeersCount(URLS.get(0)));
//        System.out.println(CLIENT.getBlocksList(URLS.get(0)));
//        System.out.println(CLIENT.getBlockchainHeight(URLS.get(0)));
//    }


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
        Random random = new Random();
        String host = URLS.get(random.nextInt(ACCOUNTS.size()));
        String recipient = (String) ACCOUNTS.keySet().toArray()[random.nextInt(ACCOUNTS.size())];
        String sender = ACCOUNTS.keySet().stream().filter(adr -> !recipient.equalsIgnoreCase(adr)).collect(Collectors.toList()).get(random.nextInt(ACCOUNTS.size() - 1));
        String secretPhrase = ACCOUNTS.get(sender);
        Long amount = nqt(random.nextInt(10) + 1);
        return CLIENT.sendMoneyTransaction(host, secretPhrase, recipient, amount);
    }
}
