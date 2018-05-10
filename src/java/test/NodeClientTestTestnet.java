package test;


import apl.AccountLedger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static test.TestData.*;
import static test.TestUtil.*;

/**
 * Test scenarios on testnet for {@link NodeClient}
 */
public class NodeClientTestTestnet extends AbstractNodeClientTest {
    public static final String MAIN_RS = "APL-NZKH-MZRE-2CTT-98NPZ";
    //transaction hash from 7446 block 20_000 APL to RS4
    public static final String TRANSACTION_HASH = "0619d7f4e0f8d2dab76f28e320c5ca819b2a08dc2294e53151bf14d318d5cefa";
    public static final String PRIVATE_TRANSACTION_HASH = "6c55253438130d20e70834ed67d7fcfc11c79528d1cdfbff3d6398bf67357fad";
    public static final String PRIVATE_TRANSACTION_SENDER = "APL-PP8M-TPRN-ARNZ-5ZUVF";
    public static final String PRIVATE_TRANSACTION_ID = "2309523316024890732";
    public static final Long BLOCK_HEIGHT = 7446L;
    public static final Long PRIVATE_BLOCK_HEIGHT = 16847L;
    public static final String PRIVATE_TRANSACTION_RECIPIENT = "APL-4QN7-PNGP-SZFV-59XZL";

    public NodeClientTestTestnet() {
        super(TEST_LOCALHOST, TEST_FILE);
    }

    @Test
    @Override
    public void testGet() throws Exception {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "getBlock");
        parameters.put("height", BLOCK_HEIGHT.toString());
        String json = client.getJson(createURI(url), parameters);
        ObjectMapper MAPPER = getMAPPER();
        Block block = MAPPER.readValue(json, Block.class);
        Assert.assertNotNull(block);
        Assert.assertEquals(BLOCK_HEIGHT.longValue(), block.getHeight().longValue());
        Assert.assertEquals(MAIN_RS, block.getGeneratorRS());
        Assert.assertEquals(3, block.getNumberOfTransactions().intValue());
        Assert.assertEquals(nqt(3).longValue(), block.getTotalFeeNQT().longValue());
        Assert.assertEquals(nqt(58_000).longValue(), block.getTotalAmountNQT().longValue());
    }


    @Test
    @Override
    public void testGetBlockchainHeight() throws Exception {
        Long blockchainHeight = client.getBlockchainHeight(url);
        Assert.assertNotNull(blockchainHeight);
        Assert.assertTrue(blockchainHeight >= BLOCK_HEIGHT); //block is already exist
    }

    @Test
    @Override
    public void testGetPeersList() throws Exception {
        List<Peer> peersList = client.getPeersList(url);
        checkList(peersList);
        Assert.assertEquals(5, peersList.size());
    }

    @Test
    @Override
    public void testGetBlocksList() throws Exception {
        List<Block> blocksList = client.getBlocksList(url, false, null);
        checkList(blocksList);
        Assert.assertEquals(5, blocksList.size());
    }

    @Test
    @Override
    public void testGetBlockTransactionsList() throws Exception {
        List<Transaction> blockTransactionsList = client.getBlockTransactionsList(url, BLOCK_HEIGHT);
        checkList(blockTransactionsList);
        Assert.assertEquals(3, blockTransactionsList.size());
        blockTransactionsList.forEach(transaction -> {
            Assert.assertEquals(BLOCK_HEIGHT, transaction.getHeight());
            Assert.assertEquals(MAIN_RS, transaction.getSenderRS());
        });
    }

    @Test
    @Override
    public void testGetPeersCount() throws Exception {
        Assert.assertEquals(5, client.getPeersCount(url));
    }


    @Test
    @Override
    public void testGetPeersIPs() throws Exception {
        List<String> peersIPs = client.getPeersIPs(url);
        checkList(peersIPs);
        peersIPs.forEach(ip -> Assert.assertTrue(IP_PATTERN.matcher(ip).matches()));
        peersIPs.forEach(ip -> {
                    boolean found = false;
                    for (String aip : URLS) {
                        String tmp = aip.substring(aip.indexOf("//") + 2, aip.lastIndexOf(":"));
                        if (tmp.equals(ip)) {
                            found = true;
                        }
                    }
                    if (!found) {
                        Assert.fail("Unknown ip: " + ip);
                    }
                }
        );
    }

    @Test
    @Override
    public void testGetTransaction() throws IOException {
        Transaction transaction = client.getTransaction(url, TRANSACTION_HASH);
        Assert.assertNotNull(transaction);
        Assert.assertEquals(TRANSACTION_HASH, transaction.getFullHash());
        Assert.assertEquals(nqt(1), transaction.getFeeNQT());
        Assert.assertEquals(nqt(20_000), transaction.getAmountNQT());
        Assert.assertFalse(transaction.isPrivate());
        Assert.assertEquals(0, transaction.getSubtype().intValue());
        Assert.assertEquals(0, transaction.getType().intValue());
        Assert.assertEquals(MAIN_RS, transaction.getSenderRS());
//        Assert.assertEquals(RS4, transaction.getRecipientRS());
    }

    @Test
    public void testGetAccountLedger() throws Exception {
        String accountRs = getRandomRS(accounts);
        List<LedgerEntry> accountLedger = client.getAccountLedger(url, accountRs, true);
        checkList(accountLedger);
        accountLedger.forEach(entry -> {
            Transaction transaction = entry.getTransaction();
            if (transaction != null && !transaction.getSenderRS().equalsIgnoreCase(accountRs) && !transaction.getRecipientRS().equalsIgnoreCase(accountRs)) {
                Assert.fail("Not this user ledger!");
            }
            if (transaction != null && transaction.isPrivate()) {
                Assert.fail("Private transactions should not be present in public ledger");
            }
        });
    }

    @Test
    public void testGetPrivateAccountLedger() throws Exception {
        String accountRs = getRandomRS(accounts);
        List<LedgerEntry> accountLedger = client.getPrivateAccountLedger(url, accounts.get(accountRs), true);
        checkList(accountLedger);
        accountLedger.forEach(entry -> {
            Transaction transaction = entry.getTransaction();
            if (transaction != null && !accountRs.equalsIgnoreCase(transaction.getSenderRS()) && !accountRs.equalsIgnoreCase(transaction.getRecipientRS())) {
                Assert.fail("Not this user ledger!");
            }
        });
        List<LedgerEntry> publicLedger = client.getAccountLedger(url, accountRs, true);
        publicLedger.forEach(accountLedger::remove);
        accountLedger.forEach(entry -> {
            if (!entry.getEventType().equals(AccountLedger.LedgerEvent.PRIVATE_PAYMENT)
                    && !entry.getEventType().equals(AccountLedger.LedgerEvent.TRANSACTION_FEE)) {
                Assert.fail("Not only private payment and theirs fee are present in accountLedger. Fail for entry: " + entry);
            }
        });
    }

    @Test
    public void testGetPrivateTransaction() throws Exception {
        Transaction privateTransaction1 = client.getPrivateTransaction(url, accounts.get(PRIVATE_TRANSACTION_SENDER), PRIVATE_TRANSACTION_HASH, null);
        Transaction privateTransaction2 = client.getPrivateTransaction(url, accounts.get(PRIVATE_TRANSACTION_SENDER), null, PRIVATE_TRANSACTION_ID);
        Assert.assertEquals(privateTransaction1, privateTransaction2);
        Assert.assertEquals(privateTransaction1.getSenderRS(), PRIVATE_TRANSACTION_SENDER);
        Assert.assertEquals(privateTransaction1.getFullHash(), PRIVATE_TRANSACTION_HASH);
        Assert.assertEquals(privateTransaction1.getRecipientRS(), "APL-8BNS-LMPW-3KHL-3B7JM");
        Assert.assertEquals(privateTransaction1.getAmountNQT(), nqt(2));
        Assert.assertEquals(privateTransaction1.getFeeNQT(), nqt(1));
        Assert.assertTrue(privateTransaction1.isPrivate());
    }

    @Test
    public void testGetPrivateTransactionFromGetTransaction() throws Exception {
        Transaction privateTransaction = client.getTransaction(url, PRIVATE_TRANSACTION_HASH);
        Assert.assertTrue(privateTransaction.isNull());
    }

    @Test
    public void testGetPrivateBlockTransactions() throws Exception {
        List<Transaction> transactionsList = client.getPrivateBlockchainTransactionsList(url, accounts.get(PRIVATE_TRANSACTION_SENDER), PRIVATE_BLOCK_HEIGHT, null, null);
        checkList(transactionsList);
        Assert.assertEquals(4, transactionsList.size());
        transactionsList.forEach(transaction -> {
            Assert.assertEquals(PRIVATE_BLOCK_HEIGHT, transaction.getHeight());
            Assert.assertEquals(PRIVATE_TRANSACTION_SENDER, transaction.getSenderRS());
        });
        List<Transaction> publicTransactions = client.getBlockTransactionsList(url, PRIVATE_BLOCK_HEIGHT);
        Assert.assertEquals(4, publicTransactions.size());
        publicTransactions.forEach(transactionsList::remove);
        Assert.assertEquals(2, transactionsList.size());
        transactionsList.forEach(transaction -> Assert.assertTrue(transaction.isPrivate()));
        Assert.assertEquals(PRIVATE_TRANSACTION_RECIPIENT, transactionsList.get(1).getRecipientRS());
    }

    @Test
    public void testGetPrivateBlockTransactionsWithPagination() throws Exception {
        List<Transaction> transactionsList1 = client.getPrivateBlockchainTransactionsList(url, accounts.get(PRIVATE_TRANSACTION_SENDER), null, 5L, 9L);
        checkList(transactionsList1);
        Assert.assertEquals(5, transactionsList1.size());
        checkAddress(transactionsList1, PRIVATE_TRANSACTION_SENDER);

        List<Transaction> transactionsList2 = client.getPrivateBlockchainTransactionsList(url, accounts.get(PRIVATE_TRANSACTION_SENDER), null, null, 10L);
        checkList(transactionsList2);
        Assert.assertEquals(11, transactionsList2.size());
        Assert.assertTrue(transactionsList2.containsAll(transactionsList1));
        checkAddress(transactionsList2, PRIVATE_TRANSACTION_SENDER);

        List<Transaction> transactionsList3 = client.getPrivateBlockchainTransactionsList(url, accounts.get(PRIVATE_TRANSACTION_SENDER), null, 5L, null);
        checkList(transactionsList3);
        Assert.assertTrue(transactionsList3.size() > 5);
        Assert.assertTrue(transactionsList3.containsAll(transactionsList1));
        checkAddress(transactionsList3, PRIVATE_TRANSACTION_SENDER);
    }

    @Test
    public void testGetBlockWithPrivateTransactions() throws Exception {
        List<Transaction> blockTransactionsList1 = client.getBlockTransactionsList(url, PRIVATE_BLOCK_HEIGHT);
        List<Transaction> blockTransactionsList2 = client.getBlockTransactionsList(url, PRIVATE_BLOCK_HEIGHT);
        checkList(blockTransactionsList1);
        checkList(blockTransactionsList2);
        Assert.assertEquals(4, blockTransactionsList1.size());
        blockTransactionsList2.forEach(blockTransactionsList1::remove);
        blockTransactionsList1.forEach(tr1 ->
            blockTransactionsList2.forEach(tr2 -> {
            if (tr2.getFullHash().equalsIgnoreCase(tr1.getFullHash())) {
                Assert.assertFalse(tr1.getSenderRS().equalsIgnoreCase(tr2.getSenderRS()));
                Assert.assertFalse(tr1.getRecipientRS().equalsIgnoreCase(tr2.getRecipientRS()));
                Assert.assertFalse(tr1.getAmountNQT().equals(tr2.getAmountNQT()));
            }
        }));
    }
}



