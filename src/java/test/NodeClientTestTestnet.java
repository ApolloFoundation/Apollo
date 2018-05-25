package test;


import apl.AccountLedger;
import apl.crypto.Crypto;
import apl.util.Convert;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static test.NodeClient.DEFAULT_DEADLINE;
import static test.NodeClient.DEFAULT_FEE;
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
    public void testGetBlockWithPrivateAndPublicTransactions() throws IOException {
        Block block1 = client.getBlock(url, PRIVATE_BLOCK_HEIGHT);
        Block block2 = client.getBlock(url, PRIVATE_BLOCK_HEIGHT);
        Assert.assertEquals(block1.getTransactions().size(), block2.getTransactions().size());
        Assert.assertEquals(4, block1.getTransactions().size());
        Assert.assertFalse(block1.getTotalAmountNQT().equals(block2.getTotalAmountNQT()));
        Assert.assertFalse(block1.getTotalAmountNQT() <= nqt(2));
        Assert.assertEquals(block1.getTotalAmountNQT().longValue(), block1.getTransactions().stream().mapToLong(Transaction::getAmountNQT).sum());
        Assert.assertEquals(block2.getTotalAmountNQT().longValue(), block2.getTransactions().stream().mapToLong(Transaction::getAmountNQT).sum());
    }

    @Test
    public void testGetBlockWithPublicTransactions() throws IOException {
        Block block = client.getBlock(url, BLOCK_HEIGHT);
        Assert.assertEquals(nqt(58_000), block.getTotalAmountNQT());
        Assert.assertEquals(nqt(58_000).longValue(), block.getTransactions().stream().mapToLong(Transaction::getAmountNQT).sum());
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
    }

    @Test
    public void testGetEncryptedPrivateBlockTransactions() throws Exception {
        String secretPhrase = accounts.get(PRIVATE_TRANSACTION_SENDER);
        byte[] publicKey = Crypto.getPublicKey(secretPhrase);
        List<Transaction> transactionsList = decryptList(client.getEncryptedPrivateBlockchainTransactionsList(url, PRIVATE_BLOCK_HEIGHT, null, null, Convert.toHexString(publicKey)), secretPhrase, "transaction", Transaction.class);
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
        decryptList(client.getEncryptedPrivateBlockchainTransactionsList(url, null, 5L, 9L, null), null, "transaction", Transaction.class);
    }

    @Test
    public void testGetEncryptedPrivateBlockchainTransactionsWithPagination() throws Exception {
        String secretPhrase = accounts.get(PRIVATE_TRANSACTION_SENDER);
        byte[] publicKey = Crypto.getPublicKey(secretPhrase);
        List<Transaction> transactionsList1 = decryptList(client.getEncryptedPrivateBlockchainTransactionsList(url, null, 5L, 9L, Convert.toHexString(publicKey)), secretPhrase, "transaction", Transaction.class);
        checkList(transactionsList1);
        Assert.assertEquals(5, transactionsList1.size());
        checkAddress(transactionsList1, PRIVATE_TRANSACTION_SENDER);

        List<Transaction> transactionsList2 = decryptList(client.getEncryptedPrivateBlockchainTransactionsList(url, null, null, 10L, Convert.toHexString(publicKey)), secretPhrase, "transaction", Transaction.class);
        checkList(transactionsList2);
        Assert.assertEquals(11, transactionsList2.size());
        Assert.assertTrue(transactionsList2.containsAll(transactionsList1));
        checkAddress(transactionsList2, PRIVATE_TRANSACTION_SENDER);

        List<Transaction> transactionsList3 = decryptList(client.getEncryptedPrivateBlockchainTransactionsList(url, null, 5L, null, Convert.toHexString(publicKey)), secretPhrase, "transaction", Transaction.class);
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
                        Assert.assertFalse(tr1.getAmountNQT().equals(tr2.getAmountNQT()));
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
            client.sendMoney(url, accounts.get(PRIVATE_TRANSACTION_SENDER), PRIVATE_TRANSACTION_RECIPIENT, nqt(i));
            TimeUnit.SECONDS.sleep(1);
            client.sendMoneyPrivateTransaction(url, accounts.get(PRIVATE_TRANSACTION_SENDER), PRIVATE_TRANSACTION_RECIPIENT, nqt(i) * 2, NodeClient.DEFAULT_FEE, NodeClient.DEFAULT_DEADLINE);
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
            Assert.assertEquals(nqt(i), transaction.getAmountNQT());
            Assert.assertFalse(transaction.isPrivate());
        }
    }

    @Test
    public void testGetPrivateUnconfirmedTransactions() throws Exception {
        String secretPhrase = accounts.get(PRIVATE_TRANSACTION_SENDER);
        prepareUnconfirmedTransactionsPool(secretPhrase);
        List<Transaction> unconfirmedTransactions = client.getPrivateUnconfirmedTransactions(url, secretPhrase, 0, 9);
        validatePrivateUnconfirmedTransactions(unconfirmedTransactions);
    }

    @Test
    public void testGetAccountLedgerEntry() throws Exception {
        LedgerEntry ledgerEntry = client.getAccountLedgerEntry(url, LEDGER_ENTRY_ID, true);
        Assert.assertEquals(AccountLedger.LedgerEvent.ORDINARY_PAYMENT, ledgerEntry.getEventType());
        Assert.assertEquals(PRIVATE_TRANSACTION_SENDER_ID.toString(), ledgerEntry.getAccount());
        Assert.assertEquals(nqt(100_000).longValue(), ledgerEntry.getChange().longValue());
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
        Assert.assertEquals(nqt(-2), ledgerEntry.getChange());
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
    public void testGetEncryptedPrivateAccountLedger() throws Exception {
        String secretPhrase = accounts.get(PRIVATE_TRANSACTION_SENDER
        );
        String publicKey = Convert.toHexString(Crypto.getPublicKey(secretPhrase));
        List<LedgerEntry> accountLedger = decryptList(client.getEncryptedPrivateAccountLedger(url, publicKey, true), secretPhrase, "ledgerEntry", LedgerEntry.class);
        checkList(accountLedger);
        accountLedger.forEach(entry -> {
            Transaction transaction = entry.getTransaction();
            if (transaction != null && !transaction.isOwnedBy(PRIVATE_TRANSACTION_SENDER)) {
                Assert.fail("Not this user ledger!");
            }
        });
        List<LedgerEntry> publicLedger = client.getAccountLedger(url,PRIVATE_TRANSACTION_SENDER, true);
        publicLedger.forEach(accountLedger::remove);
        accountLedger.forEach(entry -> {
            if (!entry.getEventType().equals(AccountLedger.LedgerEvent.PRIVATE_PAYMENT)
                    && !entry.getEventType().equals(AccountLedger.LedgerEvent.TRANSACTION_FEE)) {
                Assert.fail("Not only private payment and theirs fee are present in accountLedger. Fail for entry: " + entry);
            }
        });
    }

    private <T> List<T> decryptList(String json, String secretPhrase, String name, Class<T> objectClass) throws Exception {
        ObjectMapper mapper = getMAPPER();
        JsonNode root = mapper.readTree(json);
        Iterator<JsonNode> iterator = root.iterator();
        JsonNode objectArray = null;
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            if (node.isArray()) {
                objectArray = node;
                break;
            }
        }
        JsonNode serverPublicKey = root.get("serverPublicKey");
        if (objectArray == null || serverPublicKey == null) {
            throw new RuntimeException("Server bad response: " + root.toString());
        }
        byte[] sharedKey = Crypto.getSharedKey(Crypto.getPrivateKey(secretPhrase), Convert.parseHexString(serverPublicKey.textValue()));
        List<T> objectList = new ArrayList<>();
        if (objectArray.isArray()) {
            for (final JsonNode objectJson : objectArray) {
                String fieldName = "encrypted" + name.substring(0, 1).toUpperCase() + name.substring(1);
                JsonNode encryptedObject = objectJson.get(fieldName);
                if (encryptedObject != null) {
                    objectList.add(decrypt(encryptedObject.textValue(), sharedKey, objectClass));
                } else {
                    objectList.add(mapper.readValue(objectJson.toString(), objectClass));
                }
            }
        }
        return objectList;
    }

    private <T> T decrypt(String encryptedString, byte[] sharedKey, Class<T> objectClass) throws Exception {
        byte[] encryptedBytes = Convert.parseHexString(encryptedString);
        byte[] decryptedData = Crypto.aesDecrypt(encryptedBytes, sharedKey);
        String decryptedJson = Convert.toString(decryptedData);
        T object = getMAPPER().readValue(decryptedJson, objectClass);
        Method isPrivate = objectClass.getMethod("isPrivate");
        Object result = isPrivate.invoke(object);
        Assert.assertTrue((Boolean) result);
        return object;
    }

    private <T> T decryptObject(String json, String secretPhrase, String name, Class<T> objectClass) throws Exception {
        JsonNode root = getMAPPER().readTree(json);
        byte[] serverPublicKey = Convert.parseHexString(root.get("serverPublicKey").textValue());
        byte[] sharedKey = Crypto.getSharedKey(Crypto.getPrivateKey(secretPhrase), serverPublicKey);
        String fieldName = "encrypted" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        String encryptedJsonString = root.get(fieldName).textValue();
        return decrypt(encryptedJsonString, sharedKey, objectClass);
    }

    @Test
    public void testGetEncryptedPrivateUnconfirmedTransactions() throws Exception {
        String secretPhrase = accounts.get(PRIVATE_TRANSACTION_SENDER);
        prepareUnconfirmedTransactionsPool(secretPhrase);
        String publicKey = Convert.toHexString(Crypto.getPublicKey(secretPhrase));
        List<Transaction> unconfirmedTransactions = decryptList(client.getEncryptedPrivateUnconfirmedTransactions(url, publicKey, 0, 9), secretPhrase, "unconfirmedTransaction", Transaction.class);
        validatePrivateUnconfirmedTransactions(unconfirmedTransactions);
    }

    private void validatePrivateUnconfirmedTransactions(List<Transaction> unconfirmedTransactions) {
        checkList(unconfirmedTransactions);
        Assert.assertEquals(10, unconfirmedTransactions.size());
        for (int i = 1; i <= unconfirmedTransactions.size(); i++) {
            Transaction transaction = unconfirmedTransactions.get(i - 1);
            Assert.assertEquals(PRIVATE_TRANSACTION_SENDER, transaction.getSenderRS());
            Assert.assertEquals(PRIVATE_TRANSACTION_RECIPIENT, transaction.getRecipientRS());
            if (i % 2 != 0) {
                Assert.assertEquals(nqt(i / 2 + 1), transaction.getAmountNQT());
                Assert.assertFalse(transaction.isPrivate());
            } else {
                Assert.assertEquals(nqt(i), transaction.getAmountNQT());
                Assert.assertTrue(transaction.isPrivate());
            }
        }
    }

    private void prepareUnconfirmedTransactionsPool(String senderSecretPhrase) throws Exception{
        for (int i = 1; i <= 6; i++) {
            client.sendMoney(url, senderSecretPhrase, PRIVATE_TRANSACTION_RECIPIENT, nqt(i), DEFAULT_FEE, DEFAULT_DEADLINE);
            TimeUnit.SECONDS.sleep(1);
            client.sendMoneyPrivateTransaction(url, senderSecretPhrase, PRIVATE_TRANSACTION_RECIPIENT, nqt(i) * 2,
                    NodeClient.DEFAULT_FEE, DEFAULT_DEADLINE);
            TimeUnit.SECONDS.sleep(1);
        }
        TimeUnit.SECONDS.sleep(3);
    }

    @Test
    public void testGetEncryptedPrivateAccountLedgerEntry() throws Exception {
        String secretPhrase = accounts.get(PRIVATE_TRANSACTION_SENDER);
        String publicKey = Convert.toHexString(Crypto.getPublicKey(secretPhrase));
        LedgerEntry ledgerEntry = decryptObject(client.getEncryptedPrivateAccountLedgerEntry(url, publicKey, PRIVATE_LEDGER_ENTRY_ID, true), secretPhrase, "ledgerEntry", LedgerEntry.class);
        Assert.assertFalse(ledgerEntry.isNull());
        Assert.assertEquals(PRIVATE_LEDGER_ENTRY_ID, ledgerEntry.getLedgerId());
        Assert.assertEquals(AccountLedger.LedgerEvent.PRIVATE_PAYMENT, ledgerEntry.getEventType());
        Assert.assertEquals(nqt(-2), ledgerEntry.getChange());
        Assert.assertEquals(12150L, ledgerEntry.getHeight().longValue());
        Assert.assertEquals(9584301L, ledgerEntry.getTimestamp().longValue());
    }

    @Test
    public void testGetEncryptedPrivateTransaction() throws Exception {
        String secretPhrase = accounts.get(PRIVATE_TRANSACTION_SENDER);
        String publicKey = Convert.toHexString(Crypto.getPublicKey(secretPhrase));
        Transaction privateTransaction1 = decryptObject(client.getEncryptedPrivateTransaction(url, publicKey, PRIVATE_TRANSACTION_HASH, null), secretPhrase, "transaction", Transaction.class);
        Transaction privateTransaction2 = decryptObject(client.getEncryptedPrivateTransaction(url, publicKey, null, PRIVATE_TRANSACTION_ID), secretPhrase, "transaction", Transaction.class);
        Assert.assertEquals(privateTransaction1, privateTransaction2);
        Assert.assertEquals(privateTransaction1.getSenderRS(), PRIVATE_TRANSACTION_SENDER);
        Assert.assertEquals(privateTransaction1.getFullHash(), PRIVATE_TRANSACTION_HASH);
        Assert.assertEquals(privateTransaction1.getRecipientRS(), "APL-8BNS-LMPW-3KHL-3B7JM");
        Assert.assertEquals(privateTransaction1.getAmountNQT(), nqt(2));
        Assert.assertEquals(privateTransaction1.getFeeNQT(), nqt(1));
        Assert.assertTrue(privateTransaction1.isPrivate());
    }
}



