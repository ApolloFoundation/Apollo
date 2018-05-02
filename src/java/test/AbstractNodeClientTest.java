package test;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static test.NodeClient.DEFAULT_AMOUNT;
import static test.TestUtil.createURI;

public abstract class AbstractNodeClientTest {
    protected static final Pattern IP_PATTERN = Pattern.compile(
        "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    protected static final Map<String, String> ACCOUNTS = new HashMap<>();
    protected final String url;
    protected NodeClient client = new NodeClient();
    protected JSONParser parser = new JSONParser();

    public AbstractNodeClientTest(String url, String fileName) {
        this.url = url;
        ACCOUNTS.putAll(TestUtil.loadKeys(fileName));
    }

    protected static String getRandomRS() {
        Random random = new Random();
        return new ArrayList<>(ACCOUNTS.keySet()).get(random.nextInt(ACCOUNTS.size()));
    }

    protected static String getRandomRecipientRS(String senderRS) {
        Random random = new Random();
        return new ArrayList<>(ACCOUNTS.keySet()).stream().filter(rs -> !senderRS.equalsIgnoreCase(rs)).collect(Collectors.toList()).get(random.nextInt(ACCOUNTS.size() - 1));
    }

    @Test
    public void testPost() throws Exception {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "getAccountId");
        String randomRS = getRandomRS();
        parameters.put("secretPhrase", ACCOUNTS.get(randomRS));
        String json = client.postJson(createURI(url),
            parameters, "");
        Assert.assertNotNull(json);
        Assert.assertTrue(!json.isEmpty());
        JSONObject result = (JSONObject) parser.parse(json);
        Assert.assertEquals(randomRS, result.get("accountRS"));
    }

    @Test
    public void testGetAccountTransactionsList() throws Exception {
        String randomRS = getRandomRS();
        List<Transaction> accountTransactionsList = client.getAccountTransactionsList(url, randomRS);
        Assert.assertNotNull(accountTransactionsList);
        Assert.assertFalse(accountTransactionsList.isEmpty());
        accountTransactionsList.forEach(transaction -> {
            if (!randomRS.equalsIgnoreCase(transaction.getSenderRS()) && !randomRS.equalsIgnoreCase(transaction.getRecipientRS()))
                Assert.fail("There are not this user transactions");
        });
    }
    @Test
    public void testSendMoneyTransaction() throws Exception {
        String senderRS = getRandomRS();
        String recipientRS = getRandomRecipientRS(senderRS);
        Transaction transaction = client.sendMoneyTransaction(url, ACCOUNTS.get(senderRS), recipientRS, DEFAULT_AMOUNT);
        Assert.assertEquals(0, transaction.getType().intValue());
        Assert.assertEquals(0, transaction.getSubtype().intValue());
        Assert.assertEquals(recipientRS, transaction.getRecipientRS());
        Assert.assertEquals(senderRS, transaction.getSenderRS());
        Assert.assertEquals(60L, transaction.getDeadline().longValue());
        Assert.assertEquals(100000000L, transaction.getAmountNQT().longValue());
        Assert.assertEquals(100000000L, transaction.getFeeNQT().longValue());
    }

    @Test
    public abstract void testGet() throws Exception;

    @Test
    public abstract void testGetBlockchainHeight() throws Exception;

    @Test
    public abstract void testGetPeersList() throws Exception;

    @Test
    public abstract void testGetBlocksList() throws Exception;

    @Test
    public abstract void testGetBlockTransactionsList() throws Exception;

    @Test
    public abstract void testGetPeersCount() throws Exception;
    @Test
    public abstract void testGetPeersIPs() throws Exception;
    @Test
    public abstract void testGetTransaction() throws Exception;
}
