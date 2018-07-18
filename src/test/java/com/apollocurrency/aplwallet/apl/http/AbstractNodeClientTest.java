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

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.NodeClient;
import util.TestUtil;
import dto.Transaction;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.apollocurrency.aplwallet.apl.NodeClient.DEFAULT_AMOUNT;
import static util.TestUtil.getRandomRS;
import static util.TestUtil.getRandomRecipientRS;

public abstract class AbstractNodeClientTest {
    protected static final Pattern IP_PATTERN = Pattern.compile(
        "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    protected final Map<String, String> accounts = new HashMap<>();
    protected final String url;
    protected NodeClient client = new NodeClient();
    protected JSONParser parser = new JSONParser();

    public AbstractNodeClientTest(String url, String fileName) {
        this.url = url;
        accounts.putAll(TestUtil.loadKeys(fileName));
    }

    public void testPost() throws Exception {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "getAccountId");
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
        List<Transaction> accountTransactionsList = client.getAccountTransactionsList(url, randomRS);
        Assert.assertNotNull(accountTransactionsList);
        Assert.assertFalse(accountTransactionsList.isEmpty());
        accountTransactionsList.forEach(transaction -> {
            if (!randomRS.equalsIgnoreCase(transaction.getSenderRS()) && !randomRS.equalsIgnoreCase(transaction.getRecipientRS()))
                Assert.fail("There are not this user transactions!");
            if (transaction.isPrivate()) {
                Assert.fail("Private transactions should not appeared here!");
            }
        });
    }

    public void testSendMoneyTransaction() throws Exception {
        String senderRS = getRandomRS(accounts);
        String recipientRS = getRandomRecipientRS(accounts, senderRS);
        Transaction transaction = client.sendMoneyTransaction(url, accounts.get(senderRS), recipientRS, DEFAULT_AMOUNT);
        Assert.assertFalse(transaction.isPrivate());
        Assert.assertEquals(0, transaction.getType().intValue());
        Assert.assertEquals(0, transaction.getSubtype().intValue());
        Assert.assertEquals(recipientRS, transaction.getRecipientRS());
        Assert.assertEquals(senderRS, transaction.getSenderRS());
        Assert.assertEquals(60L, transaction.getDeadline().longValue());
        Assert.assertEquals(100000000L, transaction.getAmountATM().longValue());
        Assert.assertEquals(100000000L, transaction.getFeeATM().longValue());
    }

    public void testGetPrivateBlockchainTransactions() throws Exception {
        String account = getRandomRS(accounts);
        List<Transaction> allTransactions = client.getPrivateBlockchainTransactionsList(url, accounts.get(account), null, null, null);
        Assert.assertNotNull(allTransactions);
        client.getAccountTransactionsList(url, account).forEach(allTransactions::remove);
        allTransactions.forEach(transaction -> {
            if (!transaction.getRecipientRS().equalsIgnoreCase(account) && !transaction.getSenderRS().equalsIgnoreCase(account)) {
                Assert.fail("Not this user: " + account + " transaction " + transaction);
            }
            Assert.assertTrue(transaction.isPrivate());
        });
    }

    public void testSendMoneyPrivate() throws Exception {
        String sender = getRandomRS(accounts);
        String secretPhrase = accounts.get(sender);
        String recipient = getRandomRecipientRS(accounts, sender);
        Transaction privateTransaction = client.sendMoneyPrivateTransaction(url, secretPhrase, recipient, DEFAULT_AMOUNT, NodeClient.DEFAULT_FEE, NodeClient.DEFAULT_DEADLINE);
        Assert.assertNotNull(privateTransaction);
        Assert.assertTrue(privateTransaction.isPrivate());
    }

    protected <T> void checkList(List<T> list) {
        Assert.assertNotNull(list);
        Assert.assertFalse(list.isEmpty());
    }

    protected void checkAddress(List<Transaction> transactions, String address) {
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
