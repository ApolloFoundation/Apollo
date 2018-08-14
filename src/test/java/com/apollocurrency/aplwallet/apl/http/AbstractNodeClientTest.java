/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.JSONTransaction;
import com.apollocurrency.aplwallet.apl.NodeClient;
import com.apollocurrency.aplwallet.apl.Transaction;
import com.apollocurrency.aplwallet.apl.TransactionType;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.junit.Test;
import util.TestUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.apollocurrency.aplwallet.apl.NodeClient.DEFAULT_AMOUNT;
import static util.TestUtil.*;

public abstract class AbstractNodeClientTest {
    protected static final Pattern IP_PATTERN = Pattern.compile(
        "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    protected final Map<String, String> accounts = new HashMap<>();
    protected final String url;
    protected NodeClient client = new NodeClient();
    protected JSONParser parser = new JSONParser();
    protected List<String> peerUrls;

    public AbstractNodeClientTest(String url, String fileName, List<String> peerUrls) {
        this.url = url;
        this.peerUrls = peerUrls;
        accounts.putAll(TestUtil.loadKeys(fileName));
    }

    protected String getRandomUrl() {
        return randomUrl(peerUrls);
    }

    @Test
    public void testGetAllTransactions() throws Exception {
        int transactionsSize = 100;
        List<JSONTransaction> allTransactions = client.getAllTransactions(url, 0, transactionsSize -1);
        checkList(allTransactions);
        Assert.assertEquals(transactionsSize, allTransactions.size());

        Stream<Long> sentToSelfTransactionStream = allTransactions
                .stream()
                .filter(tr -> tr.getRecipientId() != 0 && tr.getSenderId() != 0 && tr.getRecipientId() == (tr.getSenderId()))
                .map(Transaction::getSenderId);
        Stream<Long> sendersAccountStream = allTransactions
                .stream()
                .filter(tr -> tr.getRecipientId() == 0 || tr.getSenderId() == 0 || tr.getRecipientId() !=(tr.getSenderId()))
                .map(Transaction::getSenderId);
        Stream<Long> recipientsAccountStream = allTransactions
                .stream()
                .filter(tr-> tr.getRecipientId() == 0 || tr.getSenderId() == 0 || tr.getRecipientId()!=(tr.getSenderId()))
                .map(Transaction::getRecipientId);
        Map<Long, Long> accounts =
                Stream.concat(
                        sentToSelfTransactionStream,
                        Stream.concat(
                                sendersAccountStream,
                                recipientsAccountStream))
                        .filter(Objects::nonNull)
                        .collect(Collectors
                                .groupingBy(Function.identity(), Collectors.counting()));
        accounts.forEach((acc, numberOfTransactions)-> {
                System.out.println("Account " + acc + " has " + numberOfTransactions +" / " + transactionsSize);
            if (numberOfTransactions >= transactionsSize) {
                Assert.fail("GetAllTransactions failed. Account " + acc + " has more than " + (transactionsSize - 1) + " transactions");
            }
        } );
    }

    public void testPost() throws Exception {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "getAccount");
        String randomRS = getRandomRS(accounts);
        parameters.put("secretPhrase", accounts.get(randomRS));
        String json = client.postJson(TestUtil.createURI(url),
            parameters, "");
        Assert.assertNotNull(json);
        Assert.assertTrue(!json.isEmpty());
        JSONObject result = (JSONObject) parser.parse(json);
        Assert.assertEquals(randomRS, result.get("accountRS"));
    }

    public void testGetAccountTransactionsList() throws Exception {
        String randomRS = getRandomRS(accounts);
        List<JSONTransaction> accountTransactionsList = client.getAccountTransactionsList(url, randomRS);
        Assert.assertNotNull(accountTransactionsList);
        Assert.assertFalse(accountTransactionsList.isEmpty());
        accountTransactionsList.forEach(transaction -> {
            if (!randomRS.equalsIgnoreCase(Convert.rsAccount(transaction.getSenderId())) && !randomRS.equalsIgnoreCase(Convert.rsAccount(transaction.getRecipientId())))
                Assert.fail("There are not this user transactions!");
            if (transaction.getType() == TransactionType.Payment.PRIVATE) {
                Assert.fail("Private transactions should not appeared here!");
            }
        });
    }

    public void testSendMoneyTransaction() throws Exception {
        String senderRS = getRandomRS(accounts);
        String recipientRS = getRandomRecipientRS(accounts, senderRS);
        Transaction transaction = client.sendMoneyTransaction(url, accounts.get(senderRS), recipientRS, DEFAULT_AMOUNT);
        Assert.assertEquals(0, transaction.getType().getType());
        Assert.assertEquals(0, transaction.getType().getSubtype());
        Assert.assertEquals(recipientRS, Convert.rsAccount(transaction.getRecipientId()));
        Assert.assertEquals(senderRS, Convert.rsAccount(transaction.getSenderId()));
        Assert.assertEquals(60L, transaction.getDeadline());
        Assert.assertEquals(100000000L, transaction.getAmountATM());
        Assert.assertEquals(100000000L, transaction.getFeeATM());
    }

    public void testGetPrivateBlockchainTransactions() throws Exception {
        String account = getRandomRS(accounts);
        List<JSONTransaction> allTransactions = client.getPrivateBlockchainTransactionsList(url, accounts.get(account), -1, null, null);
        Assert.assertNotNull(allTransactions);
        client.getAccountTransactionsList(url, account).forEach(allTransactions::remove);
        allTransactions.forEach(transaction -> {
            if (!Convert.rsAccount(transaction.getRecipientId()).equalsIgnoreCase(account) && !Convert.rsAccount(transaction.getSenderId()).equalsIgnoreCase(account)) {
                Assert.fail("Not this user: " + account + " transaction " + transaction);
            }
            Assert.assertEquals(TransactionType.Payment.PRIVATE, transaction.getType());
        });
    }

    public void testSendMoneyPrivate() throws Exception {
        String sender = getRandomRS(accounts);
        String secretPhrase = accounts.get(sender);
        String recipient = getRandomRecipientRS(accounts, sender);
        Transaction privateTransaction = client.sendMoneyPrivateTransaction(url, secretPhrase, recipient, DEFAULT_AMOUNT, NodeClient.DEFAULT_FEE, NodeClient.DEFAULT_DEADLINE);
        Assert.assertNotNull(privateTransaction);
        Assert.assertEquals(TransactionType.Payment.PRIVATE, privateTransaction.getType());
    }

    protected <T> void checkList(List<T> list) {
        Assert.assertNotNull(list);
        Assert.assertFalse(list.isEmpty());
    }

    protected void checkAddress(List<JSONTransaction> transactions, String address) {
        transactions.forEach(transaction -> {
            if (!transaction.getSenderRS().equalsIgnoreCase(address) && !transaction.getRecipientRS().equalsIgnoreCase(address)) {
                Assert.fail(transaction.toString() + " is not for this address \'" + address + "\'");
            }
        });
    }

    public abstract void testGet() throws Exception;

    public abstract void testGetBlockchainHeight() throws Exception;

    public abstract void testGetPeersList() throws Exception;

    public abstract void testGetBlocksList() throws Exception;

    public abstract void testGetBlockTransactionsList() throws Exception;

    public abstract void testGetPeersCount() throws Exception;

    public abstract void testGetPeersIPs() throws Exception;

    public abstract void testGetTransaction() throws Exception;
}
