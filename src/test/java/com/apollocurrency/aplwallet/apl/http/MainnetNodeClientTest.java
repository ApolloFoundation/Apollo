/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.JSONTransaction;
import com.apollocurrency.aplwallet.apl.NodeClient;
import com.apollocurrency.aplwallet.apl.TestData;
import com.apollocurrency.aplwallet.apl.TransactionType;
import dto.Block;
import dto.Peer;
import org.eclipse.jetty.util.StringUtil;
import org.junit.*;
import util.TestUtil;
import util.WalletRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;

/**
 * Test scenarios on mainnet for {@link NodeClient}
 */
public class MainnetNodeClientTest extends AbstractNodeClientTest {
    private static final String TRANSACTION_HASH = "5e359e83f4591433bd1e6b59b06ceecd0a4731ea8b50bc76df2a9dc6c16c5f3a";
    private static WalletRunner runner = new WalletRunner(false);

    public MainnetNodeClientTest() {
        super(TestData.MAIN_LOCALHOST, TestData.MAIN_FILE, runner.getUrls());
    }

    @Override
    @Ignore
    public void testSendMoneyTransaction() throws Exception {
        super.testSendMoneyTransaction();
    }

    @Override
    @Ignore
    public void testSendMoneyPrivate() throws Exception {
        super.testSendMoneyPrivate();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        runner.shutdown();
    }

    @BeforeClass
    public static void setUp() throws Exception {
        runner.run();
    }

    @Test
    @Override
    public void testGet() throws Exception {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "getBlock");
        parameters.put("height", "10000");
        String json = client.getJson(TestUtil.createURI(url), parameters);
        assertThatJson(json)
            .isPresent()
            .node("height")
            .isPresent()
            .isEqualTo(10000);
        assertThatJson(json)
            .node("numberOfTransactions")
            .isEqualTo("0");
        assertThatJson(json)
            .node("totalFeeATM")
            .isStringEqualTo("0");
        assertThatJson(json)
            .node("totalAmountATM")
            .isStringEqualTo("0");
    }

    @Test
    @Override
    public void testGetBlockchainHeight() throws Exception {
        int blockchainHeight = client.getBlockchainHeight(url);
        Assert.assertTrue(blockchainHeight > 0);
    }

    @Test
    public void testGetPeers() throws Exception {
        String peers = client.getPeers(url);
        Assert.assertNotNull(peers);
        Assert.assertFalse(peers.isEmpty());
        assertThatJson(peers)
            .node("peers")
            .isPresent()
            .isArray();
    }

    @Test
    @Ignore
    @Override
    public void testGetPeersList() throws Exception {
        List<Peer> peersList = client.getPeersList(url);
        checkList(peersList);
        Assert.assertTrue(peersList.size() > 5);
    }

    @Test
    public void testGetBlocks() throws Exception {
        String json = client.getBlocks(url);
        Assert.assertTrue(StringUtil.isNotBlank(json));
        assertThatJson(json)
            .node("blocks")
            .isPresent()
            .isArray()
            .ofLength(5);
    }

    @Test
    @Override
    public void testGetBlocksList() throws Exception {
        List<Block> blocksList = client.getBlocksList(url,false, null);
        checkList(blocksList);
        Assert.assertEquals(5, blocksList.size());
    }

    @Test
    public void testGetBlockTransactions() throws Exception {
        int height = 106070;
        String blockTransactions = client.getBlockTransactions(url, height);
        Assert.assertTrue(StringUtil.isNotBlank(blockTransactions));
        assertThatJson(blockTransactions)
            .isPresent()
            .isArray()
            .ofLength(3);
        assertThatJson(blockTransactions)
            .node("[0].height")
            .isPresent()
            .isEqualTo(height);
    }

    @Test
    @Override
    public void testGetBlockTransactionsList() throws Exception {
        int height = 106070;
        List<JSONTransaction> blockTransactionsList = client.getBlockTransactionsList(url, height);
        checkList(blockTransactionsList);
        blockTransactionsList.forEach(transaction -> Assert.assertEquals(height, (long) transaction.getHeight()));
    }

    @Test
    @Override
    public void testGetPeersCount() throws Exception {
        Assert.assertTrue(client.getPeersCount(url) > 0);
    }

    @Test
    @Ignore
    public void testSendMoney() {
        String senderRS = TestUtil.getRandomRS(accounts);
        String recipientRS = TestUtil.getRandomRecipientRS(accounts, senderRS);
        String transaction = client.sendMoney(url, accounts.get(senderRS), recipientRS);
        Assert.assertTrue(StringUtil.isNotBlank(transaction));
        assertThatJson(transaction)
            .isPresent()
            .node("signatureHash")
            .isPresent();
        assertThatJson(transaction)
            .node("transactionJSON")
            .isPresent()
            .isObject();
        assertThatJson(transaction)
            .node("transactionJSON.amountATM")
            .isPresent()
            .isStringEqualTo(String.valueOf(TestUtil.atm(1)));
        assertThatJson(transaction)
            .node("transactionJSON.feeATM")
            .isPresent()
            .isStringEqualTo(String.valueOf(TestUtil.atm(1)));
        assertThatJson(transaction)
            .node("transactionJSON.recipientRS")
            .isPresent()
            .isStringEqualTo(recipientRS);
        assertThatJson(transaction)
            .node("transactionJSON.senderRS")
            .isPresent()
            .isStringEqualTo(senderRS);
        assertThatJson(transaction)
            .node("transactionJSON.type")
            .isPresent()
            .isEqualTo(0);
        assertThatJson(transaction)
            .node("transactionJSON.subtype")
            .isPresent()
            .isEqualTo(0);
    }

    @Test
    @Override
    public void testGetPeersIPs() throws Exception {
        List<String> peersIPs = client.getPeersIPs(url);
        checkList(peersIPs);
        peersIPs.forEach(ip -> Assert.assertTrue(IP_PATTERN.matcher(ip).matches()));
    }

    @Test
    @Override
    public void testGetTransaction() throws IOException {
        JSONTransaction transaction = client.getTransaction(url, TRANSACTION_HASH);
        Assert.assertNotNull(transaction);
        Assert.assertEquals(TRANSACTION_HASH, transaction.getFullHash());
        Assert.assertEquals(TestUtil.atm(1), transaction.getFeeATM());
        Assert.assertEquals(TestUtil.atm(902000), transaction.getAmountATM());
        Assert.assertEquals(TransactionType.Payment.ORDINARY, transaction.getType());
        Assert.assertEquals("APL-NZKH-MZRE-2CTT-98NPZ", transaction.getSenderRS());
    }

}


