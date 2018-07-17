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
import com.apollocurrency.aplwallet.apl.TestData;
import util.TestUtil;
import dto.Block;
import dto.Peer;
import dto.Transaction;
import org.eclipse.jetty.util.StringUtil;
import org.junit.*;
import util.WalletRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;

/**
 * Test scenarios on mainnet for {@link NodeClient}
 */
public class MainnetNodeClientTest extends AbstractNodeClientTest {
    private static final String TRANSACTION_HASH = "5e359e83f4591433bd1e6b59b06ceecd0a4731ea8b50bc76df2a9dc6c16c5f3a";
    private static WalletRunner runner = new WalletRunner(false);

    public MainnetNodeClientTest() {
        super(TestData.MAIN_LOCALHOST, TestData.MAIN_FILE);
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
        Long blockchainHeight = client.getBlockchainHeight(url);
        Assert.assertNotNull(blockchainHeight);
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

    @Ignore
    @Test
    @Override
    public void testGetPeersList() throws Exception {
        TimeUnit.SECONDS.sleep(10);
        List<Peer> peersList = client.getPeersList(url);
        checkList(peersList);
        Assert.assertTrue(peersList.size() > Math.ceil(0.51 * runner.getUrls().size()));
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
        long height = 106070L;
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
        long height = 106070L;
        List<Transaction> blockTransactionsList = client.getBlockTransactionsList(url, height);
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
            .isStringEqualTo(TestUtil.atm(1).toString());
        assertThatJson(transaction)
            .node("transactionJSON.feeATM")
            .isPresent()
            .isStringEqualTo(TestUtil.atm(1).toString());
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
        Transaction transaction = client.getTransaction(url, TRANSACTION_HASH);
        Assert.assertNotNull(transaction);
        Assert.assertEquals(TRANSACTION_HASH, transaction.getFullHash());
        Assert.assertEquals(TestUtil.atm(1), transaction.getFeeATM());
        Assert.assertEquals(TestUtil.atm(902000), transaction.getAmountATM());
        Assert.assertEquals(0, transaction.getSubtype().intValue());
        Assert.assertEquals(0, transaction.getType().intValue());
        Assert.assertEquals("APL-NZKH-MZRE-2CTT-98NPZ", transaction.getSenderRS());
    }

}


