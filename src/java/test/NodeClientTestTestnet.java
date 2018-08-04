/*
 * Copyright © 2017-2018 Apollo Foundation
 */


package test;


import apl.AccountLedger;
import apl.crypto.Crypto;
import apl.util.Convert;
import apl.TransactionType;
import apl.Version;
import apl.updater.Architecture;
import apl.updater.Platform;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import test.dto.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static test.NodeClient.DEFAULT_DEADLINE;
import static test.NodeClient.DEFAULT_FEE;
import static test.TestData.*;
import static test.TestUtil.*;

/**
 * Test scenarios on testnet for {@link NodeClient}
 */
public class NodeClientTestTestnet extends AbstractNodeClientTest {
    //transaction hash from 7446 block 20_000 APL to RS4
    public static final String TRANSACTION_HASH = "0619d7f4e0f8d2dab76f28e320c5ca819b2a08dc2294e53151bf14d318d5cefa";
    public static final String PRIVATE_TRANSACTION_HASH = "6c55253438130d20e70834ed67d7fcfc11c79528d1cdfbff3d6398bf67357fad";
    public static final String PRIVATE_TRANSACTION_SENDER = "APL-PP8M-TPRN-ARNZ-5ZUVF";
    public static final Long PRIVATE_TRANSACTION_SENDER_ID = 3958487933422064851L;
    public static final String PRIVATE_TRANSACTION_ID = "2309523316024890732";
    public static final Long BLOCK_HEIGHT = 7446L;
    public static final Long PRIVATE_BLOCK_HEIGHT = 16847L;
    public static final String PRIVATE_TRANSACTION_RECIPIENT = "APL-4QN7-PNGP-SZFV-59XZL";
    public static final Long PRIVATE_LEDGER_ENTRY_ID = 194L;
    public static final Long LEDGER_ENTRY_ID = 164L;

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
        Assert.assertEquals(atm(3).longValue(), block.getTotalFeeATM().longValue());
        Assert.assertEquals(atm(58_000).longValue(), block.getTotalAmountATM().longValue());
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
    public void testGetBlockWithPrivateAndPublicTransactions() throws IOException {
        Block block1 = client.getBlock(url, PRIVATE_BLOCK_HEIGHT);
        Block block2 = client.getBlock(url, PRIVATE_BLOCK_HEIGHT);
        Assert.assertEquals(block1.getTransactions().size(), block2.getTransactions().size());
        Assert.assertEquals(4, block1.getTransactions().size());
        Assert.assertFalse(block1.getTotalAmountATM().equals(block2.getTotalAmountATM()));
        Assert.assertFalse(block1.getTotalAmountATM() <= atm(2));
        Assert.assertEquals(block1.getTotalAmountATM().longValue(), block1.getTransactions().stream().mapToLong(Transaction::getAmountATM).sum());
        Assert.assertEquals(block2.getTotalAmountATM().longValue(), block2.getTransactions().stream().mapToLong(Transaction::getAmountATM).sum());
    }

    @Test
    public void testGetBlockWithPublicTransactions() throws IOException {
        Block block = client.getBlock(url, BLOCK_HEIGHT);
        Assert.assertEquals(atm(58_000), block.getTotalAmountATM());
        Assert.assertEquals(atm(58_000).longValue(), block.getTransactions().stream().mapToLong(Transaction::getAmountATM).sum());
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
        Assert.assertEquals(atm(1), transaction.getFeeATM());
        Assert.assertEquals(atm(20_000), transaction.getAmountATM());
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
        Assert.assertEquals(privateTransaction1.getAmountATM(), atm(2));
        Assert.assertEquals(privateTransaction1.getFeeATM(), atm(1));
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
    }

    @Test
    public void testGetEncryptedPrivateBlockTransactions() throws Exception {
        String secretPhrase = accounts.get(PRIVATE_TRANSACTION_SENDER);
        byte[] publicKey = Crypto.getPublicKey(secretPhrase);
        List<Transaction> transactionsList = client.getEncryptedPrivateBlockchainTransactionsList(url, PRIVATE_BLOCK_HEIGHT, null, null, secretPhrase, Convert.toHexString(publicKey));
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
    }

    @Test(expected = RuntimeException.class)
    public void testGetEncryptedTransactionWithoutPublicKeyAndSecretPhrase() throws Exception {
        client.getEncryptedPrivateBlockchainTransactionsList(url, PRIVATE_BLOCK_HEIGHT, null, null, null, null);
    }

    @Test
    public void testGetEncryptedPrivateBlockchainTransactionsWithPagination() throws Exception {
        String secretPhrase = accounts.get(PRIVATE_TRANSACTION_SENDER);
        byte[] publicKey = Crypto.getPublicKey(secretPhrase);
        List<Transaction> transactionsList1 = client.getEncryptedPrivateBlockchainTransactionsList(url, null, 5L, 9L, secretPhrase, Convert.toHexString(publicKey));
        checkList(transactionsList1);
        Assert.assertEquals(5, transactionsList1.size());
        checkAddress(transactionsList1, PRIVATE_TRANSACTION_SENDER);

        List<Transaction> transactionsList2 = client.getEncryptedPrivateBlockchainTransactionsList(url, null, null, 10L, secretPhrase, Convert.toHexString(publicKey));
        checkList(transactionsList2);
        Assert.assertEquals(11, transactionsList2.size());
        Assert.assertTrue(transactionsList2.containsAll(transactionsList1));
        checkAddress(transactionsList2, PRIVATE_TRANSACTION_SENDER);

        List<Transaction> transactionsList3 = client.getEncryptedPrivateBlockchainTransactionsList(url, null, 5L, null, secretPhrase, Convert.toHexString(publicKey));
        checkList(transactionsList3);
        Assert.assertTrue(transactionsList3.size() > 5);
        Assert.assertTrue(transactionsList3.containsAll(transactionsList1));
        checkAddress(transactionsList3, PRIVATE_TRANSACTION_SENDER);
    }

    @Test
    public void testGetEncryptedPrivateBlockTransactionsWithPagination() throws Exception {
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
                        Assert.assertFalse(tr1.getAmountATM().equals(tr2.getAmountATM()));
                    }
                }));
    }

    @Test
    public void testGetTransactionsWithPagination() throws Exception {
        List<Transaction> accountTransactions = client.getAccountTransactions(url, PRIVATE_TRANSACTION_SENDER, 1, 15, -1, -100);
        checkList(accountTransactions);
        Assert.assertEquals(15, accountTransactions.size());
        accountTransactions.forEach(transaction -> {
            transaction.isOwnedBy(PRIVATE_TRANSACTION_SENDER);
            Assert.assertFalse(transaction.isPrivate());
        });
    }

    @Test
    public void testAccountLedgerWithPagination() throws Exception {
        List<LedgerEntry> accountLedgerEntries = client.getAccountLedger(url, PRIVATE_TRANSACTION_SENDER, true, 1, 15);
        checkList(accountLedgerEntries);
        Assert.assertEquals(15, accountLedgerEntries.size());
        accountLedgerEntries.forEach(ledgerEntry -> {
            Assert.assertEquals(PRIVATE_TRANSACTION_SENDER_ID.longValue(), Long.parseLong(ledgerEntry.getAccount()));
            Assert.assertFalse(ledgerEntry.isPrivate());
        });
    }

    @Test
    public void testGetUnconfirmedTransactions() throws Exception {
        for (int i = 1; i <= 6; i++) {
            client.sendMoney(url, accounts.get(PRIVATE_TRANSACTION_SENDER), PRIVATE_TRANSACTION_RECIPIENT, atm(i));
            TimeUnit.SECONDS.sleep(1);
            client.sendMoneyPrivateTransaction(url, accounts.get(PRIVATE_TRANSACTION_SENDER), PRIVATE_TRANSACTION_RECIPIENT, atm(i) * 2, NodeClient.DEFAULT_FEE, NodeClient.DEFAULT_DEADLINE);
            TimeUnit.SECONDS.sleep(1);
        }
        TimeUnit.SECONDS.sleep(3);
        List<Transaction> unconfirmedTransactions = client.getUnconfirmedTransactions(url, PRIVATE_TRANSACTION_SENDER, 0, 3);
        checkList(unconfirmedTransactions);
        Assert.assertEquals(4, unconfirmedTransactions.size());
        for (int i = 1; i <= unconfirmedTransactions.size(); i++) {
            Transaction transaction = unconfirmedTransactions.get(i - 1);
            Assert.assertEquals(PRIVATE_TRANSACTION_SENDER, transaction.getSenderRS());
            Assert.assertEquals(PRIVATE_TRANSACTION_RECIPIENT, transaction.getRecipientRS());
            Assert.assertEquals(atm(i), transaction.getAmountATM());
            Assert.assertFalse(transaction.isPrivate());
        }
    }

    @Test
    public void testGetPrivateUnconfirmedTransactions() throws Exception {
        for (int i = 1; i <= 6; i++) {
            client.sendMoney(url, accounts.get(PRIVATE_TRANSACTION_SENDER), PRIVATE_TRANSACTION_RECIPIENT, atm(i), DEFAULT_FEE, DEFAULT_DEADLINE);
            TimeUnit.SECONDS.sleep(1);
            client.sendMoneyPrivateTransaction(url, accounts.get(PRIVATE_TRANSACTION_SENDER), PRIVATE_TRANSACTION_RECIPIENT, atm(i) * 2,
                    NodeClient.DEFAULT_FEE, DEFAULT_DEADLINE);
            TimeUnit.SECONDS.sleep(1);
        }
        TimeUnit.SECONDS.sleep(3);
        List<Transaction> unconfirmedTransactions = client.getPrivateUnconfirmedTransactions(url, accounts.get(PRIVATE_TRANSACTION_SENDER), 0, 9);
        checkList(unconfirmedTransactions);
        Assert.assertEquals(10, unconfirmedTransactions.size());
        for (int i = 1; i <= unconfirmedTransactions.size(); i++) {
            Transaction transaction = unconfirmedTransactions.get(i - 1);
            Assert.assertEquals(PRIVATE_TRANSACTION_SENDER, transaction.getSenderRS());
            Assert.assertEquals(PRIVATE_TRANSACTION_RECIPIENT, transaction.getRecipientRS());
            if (i % 2 != 0) {
                Assert.assertEquals(atm(i / 2 + 1), transaction.getAmountATM());
                Assert.assertFalse(transaction.isPrivate());
            } else {
                Assert.assertEquals(atm(i), transaction.getAmountATM());
                Assert.assertTrue(transaction.isPrivate());
            }
        }
    }

    @Test
    public void testGetAccountLedgerEntry() throws Exception {
        LedgerEntry ledgerEntry = client.getAccountLedgerEntry(url, LEDGER_ENTRY_ID, true);
        Assert.assertEquals(AccountLedger.LedgerEvent.ORDINARY_PAYMENT, ledgerEntry.getEventType());
        Assert.assertEquals(PRIVATE_TRANSACTION_SENDER_ID.toString(), ledgerEntry.getAccount());
        Assert.assertEquals(atm(100_000).longValue(), ledgerEntry.getChange().longValue());
        Assert.assertEquals(164L, ledgerEntry.getLedgerId().longValue());
        Assert.assertEquals(MAIN_RS, ledgerEntry.getTransaction().getSenderRS());
    }

    @Test
    public void testGetUnknownAccountLedgerEntry() throws Exception {
        LedgerEntry ledgerEntry = client.getAccountLedgerEntry(url, PRIVATE_LEDGER_ENTRY_ID, true);
        Assert.assertTrue(ledgerEntry.isNull());
    }

    @Test
    public void testGetPrivateAccountLedgerEntry() throws Exception {
        LedgerEntry ledgerEntry = client.getPrivateAccountLedgerEntry(url, accounts.get(PRIVATE_TRANSACTION_SENDER), PRIVATE_LEDGER_ENTRY_ID, true);
        Assert.assertFalse(ledgerEntry.isNull());
        Assert.assertEquals(PRIVATE_LEDGER_ENTRY_ID, ledgerEntry.getLedgerId());
        Assert.assertEquals(AccountLedger.LedgerEvent.PRIVATE_PAYMENT, ledgerEntry.getEventType());
        Assert.assertEquals(atm(-2), ledgerEntry.getChange());
        Assert.assertEquals(12150L, ledgerEntry.getHeight().longValue());
        Assert.assertEquals(9584301L, ledgerEntry.getTimestamp().longValue());
    }

    @Test
    public void testGetTransactionsWithType() throws IOException {
        List<Transaction> transactions = client.getAccountTransactions(url, PRIVATE_TRANSACTION_SENDER, 0, 99, 0, -1);
        checkList(transactions);
        Assert.assertEquals(100, transactions.size());
        transactions.forEach(transaction -> {
            Assert.assertFalse(transaction.isPrivate());
            Assert.assertEquals(0, transaction.getType().intValue());
            if (!transaction.isOwnedBy(PRIVATE_TRANSACTION_SENDER)) {
                Assert.fail("Not this user: " + PRIVATE_TRANSACTION_SENDER + ", transaction: " + transaction.toString());
            }
        });
    }

    @Test
    public void testGetTransactionsWithSubtype() throws IOException {
        List<Transaction> transactions = client.getAccountTransactions(url, PRIVATE_TRANSACTION_SENDER, 0, 49, -71, 1);
        checkList(transactions);
        Assert.assertEquals(50, transactions.size());
        transactions.forEach(transaction -> {
            Assert.assertFalse(transaction.isPrivate());
            if (!transaction.isOwnedBy(PRIVATE_TRANSACTION_SENDER)) {
                Assert.fail("Not this user: " + PRIVATE_TRANSACTION_SENDER + ", transaction: " + transaction.toString());
            }

        });
    }

    @Test
    public void testGetTransactionsWithTypeAndSubtype() throws IOException {
        List<Transaction> transactions = client.getAccountTransactions(url, PRIVATE_TRANSACTION_SENDER, 0, 49, 0, 0);
        checkList(transactions);
        Assert.assertEquals(50, transactions.size());
        transactions.forEach(transaction -> {
            Assert.assertFalse(transaction.isPrivate());
            Assert.assertTrue(transaction.isOwnedBy(PRIVATE_TRANSACTION_SENDER));
            if (!transaction.isOwnedBy(PRIVATE_TRANSACTION_SENDER)) {
                Assert.fail("Not this user: " + PRIVATE_TRANSACTION_SENDER + ", transaction: " + transaction.toString());
            }
        });
    }

    @Test
    public void testGetPrivateTransactionsThroughGetPublicTransactions() throws IOException {
        List<Transaction> transactions1 = client.getAccountTransactions(url, PRIVATE_TRANSACTION_SENDER, 0, 49, 0, 1);
        Assert.assertTrue("List contains Private transactions!", transactions1.isEmpty());
        //do request for each type
        for (int i = 0; i < 20; i++) {
            List<Transaction> transactions2 = client.getAccountTransactions(url, PRIVATE_TRANSACTION_SENDER, 0, 49, i, -1);
            int type = i;
            transactions2.forEach(transaction -> {
                Assert.assertTrue("Not this user: " + PRIVATE_TRANSACTION_SENDER + " transaction: " + transaction, transaction.isOwnedBy(PRIVATE_TRANSACTION_SENDER));
                Assert.assertFalse("Transaction is private! " + transaction + ". Type: " + type, transaction.isPrivate());
            });
            //do request for each subtype
            for (int j = 0; j < 20; j++) {
                List<Transaction> transactions3 = client.getAccountTransactions(url, PRIVATE_TRANSACTION_SENDER, 0, 49, i, j);
                int subtype = j;
                transactions3.forEach(transaction -> {
                    Assert.assertTrue("Not this user: " + PRIVATE_TRANSACTION_SENDER + " transaction: " + transaction, transaction.isOwnedBy(PRIVATE_TRANSACTION_SENDER));
                    Assert.assertFalse("Transaction is private! " + transaction + ". Type: " + type + ". Subtype: " + subtype, transaction.isPrivate());
                });
            }
        }
    }

    @Test
    public void testSendUpdateTransaction() throws IOException {
        String updateUrl = "http://apollocurrncy.com/download/linux/desktop-wallet/Apollo-1.10.11.jar";
        String hash = "0987654321098765432109876543210909876543210987654321098765432109";
        String signature = "1234567890123456789012345678901212345678901234567890123456789012";
        Platform platform = Platform.WINDOWS;
        Architecture architecture = Architecture.AMD64;
        Version version = Version.from("10000.0.0");
        UpdateTransaction updateTransaction = client.sendUpdateTransaction(url, accounts.get(PRIVATE_TRANSACTION_SENDER), 100_000_000, 0, updateUrl, version, architecture, platform, hash, signature, 5);
        UpdateTransaction.UpdateAttachment expectedAttachment = new UpdateTransaction.UpdateAttachment();
        expectedAttachment.setArchitecture(Architecture.AMD64);
        expectedAttachment.setHash(hash);
        expectedAttachment.setSignature(signature);
        expectedAttachment.setPlatform(platform);
        expectedAttachment.setVersion(version);
        expectedAttachment.setUrl(updateUrl);
        Assert.assertEquals(8, updateTransaction.getType().intValue());
        Assert.assertEquals(0, updateTransaction.getSubtype().intValue());
        Assert.assertEquals(TransactionType.Update.CRITICAL, TransactionType.findTransactionType(updateTransaction.getType(), updateTransaction.getSubtype()));
        Assert.assertNull(updateTransaction.getRecipientRS());
        Assert.assertEquals(100_000_000, updateTransaction.getFeeATM().intValue());
        Assert.assertEquals(expectedAttachment, updateTransaction.getAttachment());
    }

    @Test
    public void testGetAccountLedgerWithoutParameters() throws Exception {
        List<LedgerEntry> accountLedger = client.getAccountLedger(url, "", false, 0, 4);
        checkList(accountLedger);
        Assert.assertEquals(5, accountLedger.size());
        accountLedger.forEach(LedgerEntry::isNotPrivate);
    }
}



