/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.*;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.updater.Architecture;
import com.apollocurrency.aplwallet.apl.updater.DoubleByteArrayTuple;
import com.apollocurrency.aplwallet.apl.updater.Platform;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.fasterxml.jackson.databind.ObjectMapper;
import dto.Account;
import dto.Block;
import dto.LedgerEntry;
import dto.Peer;
import org.json.simple.parser.ParseException;
import org.junit.*;
import util.TestUtil;
import util.WalletRunner;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.apollocurrency.aplwallet.apl.TestData.TEST_FILE;
import static util.TestUtil.*;

/**
 * Test scenarios on testnet for {@link NodeClient}
 */
public class TestnetNodeClientTest extends AbstractNodeClientTest {
    public static final String TRANSACTION_HASH = "0619d7f4e0f8d2dab76f28e320c5ca819b2a08dc2294e53151bf14d318d5cefa";
    public static final String PRIVATE_TRANSACTION_HASH = "6c55253438130d20e70834ed67d7fcfc11c79528d1cdfbff3d6398bf67357fad";
    public static final String PRIVATE_TRANSACTION_SENDER = "APL-PP8M-TPRN-ARNZ-5ZUVF";
    public static final String MESSAGE_SENDER = "APL-KL45-8GRF-BKPM-E58NH";

    public static final Long PRIVATE_TRANSACTION_SENDER_ID = 3958487933422064851L;
    public static final String PRIVATE_TRANSACTION_ID = "2309523316024890732";
    public static final int BLOCK_HEIGHT = 7446;
    public static final int PRIVATE_BLOCK_HEIGHT = 16847;
    public static final String PRIVATE_TRANSACTION_RECIPIENT = "APL-4QN7-PNGP-SZFV-59XZL";

    public TestnetNodeClientTest() {
        super(TestData.TEST_LOCALHOST, TEST_FILE, runner.getUrls());
    }

    private static WalletRunner runner = new WalletRunner();

    @BeforeClass
    public static void setUp() throws Exception {
        runner.run();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        runner.shutdown();
    }

    @Test
    @Override
    public void testGet() throws Exception {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestType", "getBlock");
        parameters.put("height", String.valueOf(BLOCK_HEIGHT));
        String json = client.getJson(TestUtil.createURI(url), parameters);
        ObjectMapper MAPPER = TestUtil.getMAPPER();
        Block block = MAPPER.readValue(json, Block.class);
        Assert.assertNotNull(block);
        Assert.assertEquals(BLOCK_HEIGHT, block.getHeight());
        Assert.assertEquals(TestData.MAIN_RS, block.getGeneratorRS());
        Assert.assertEquals(3, block.getNumberOfTransactions().intValue());
        Assert.assertEquals(atm(3), block.getTotalFeeATM().longValue());
        Assert.assertEquals(atm(58_000), block.getTotalAmountATM().longValue());
    }


    @Test
    @Override
    public void testGetBlockchainHeight() throws Exception {
        int blockchainHeight = client.getBlockchainHeight(url);
        Assert.assertTrue(blockchainHeight >= BLOCK_HEIGHT); //block is already exist
    }

    @Test
    @Override
    public void testGetPeersList() throws Exception {
        List<Peer> peersList = client.getPeersList(url);
        checkList(peersList);
        Assert.assertTrue(peersList.size() > Math.ceil(0.51 * runner.getUrls().size()));
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
        Assert.assertNotSame(block1.getTotalAmountATM(), block2.getTotalAmountATM());
        Assert.assertFalse(block1.getTotalAmountATM() <= atm(2));
        Assert.assertEquals(block1.getTotalAmountATM().longValue(), block1.getTransactions().stream().mapToLong(JSONTransaction::getAmountATM).sum());
        Assert.assertEquals(block2.getTotalAmountATM().longValue(), block2.getTransactions().stream().mapToLong(JSONTransaction::getAmountATM).sum());
    }

    @Test
    public void testGetBlockWithPublicTransactions() throws IOException {
        Block block = client.getBlock(url, BLOCK_HEIGHT);
        Assert.assertEquals(atm(58_000), block.getTotalAmountATM().longValue());
        Assert.assertEquals(atm(58_000), block.getTransactions().stream().mapToLong(JSONTransaction::getAmountATM).sum());
    }

    @Test
    @Override
    public void testGetBlockTransactionsList() throws Exception {
        List<JSONTransaction> blockTransactionsList = client.getBlockTransactionsList(url, BLOCK_HEIGHT);
        checkList(blockTransactionsList);
        Assert.assertEquals(3, blockTransactionsList.size());
        blockTransactionsList.forEach(transaction -> {
            Assert.assertEquals(BLOCK_HEIGHT, transaction.getHeight());
            Assert.assertEquals(TestData.MAIN_RS, transaction.getSenderRS());
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
                    for (String aip : runner.getUrls()) {
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
        JSONTransaction transaction = client.getTransaction(url, TRANSACTION_HASH);
        Assert.assertNotNull(transaction);
        Assert.assertEquals(TRANSACTION_HASH, transaction.getFullHash());
        Assert.assertEquals(atm(1), transaction.getFeeATM());
        Assert.assertEquals(atm(20_000), transaction.getAmountATM());
        Assert.assertFalse(transaction.isPrivate());
        Assert.assertEquals(TransactionType.Payment.ORDINARY, transaction.getType());
        Assert.assertEquals(TestData.MAIN_RS, transaction.getSenderRS());
//        Assert.assertEquals(RS4, transaction.getRecipientRS());
    }

    @Test
    public void testGetAccountLedger() throws Exception {
        String accountRs = getRandomRS(accounts);
        List<LedgerEntry> accountLedger = client.getAccountLedger(url, accountRs, true);
        checkList(accountLedger);
        accountLedger.forEach(entry -> {
            JSONTransaction transaction = entry.getTransaction();
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
        List<LedgerEntry> accountLedger = client.getPrivateAccountLedger(url, accounts.get(accountRs), true, 0, 49);
        checkList(accountLedger);
        accountLedger.forEach(entry -> {
            JSONTransaction transaction = entry.getTransaction();
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
        JSONTransaction privateTransaction1 = client.getPrivateTransaction(url, accounts.get(PRIVATE_TRANSACTION_SENDER), PRIVATE_TRANSACTION_HASH,
                null);
        JSONTransaction privateTransaction2 = client.getPrivateTransaction(url, accounts.get(PRIVATE_TRANSACTION_SENDER), null,
                PRIVATE_TRANSACTION_ID);
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
        Assert.assertNull(client.getTransaction(url, PRIVATE_TRANSACTION_HASH));
    }

    @Test
    public void testGetPrivateBlockTransactions() throws Exception {
        List<JSONTransaction> transactionsList = client.getPrivateBlockchainTransactionsList(url, accounts.get(PRIVATE_TRANSACTION_SENDER), PRIVATE_BLOCK_HEIGHT, null, null);
        checkList(transactionsList);
        Assert.assertEquals(4, transactionsList.size());
        transactionsList.forEach(transaction -> {
            Assert.assertEquals(PRIVATE_BLOCK_HEIGHT, transaction.getHeight());
            Assert.assertEquals(PRIVATE_TRANSACTION_SENDER, transaction.getSenderRS());
        });
        List<JSONTransaction> publicTransactions = client.getBlockTransactionsList(url, PRIVATE_BLOCK_HEIGHT);
        Assert.assertEquals(4, publicTransactions.size());
        publicTransactions.forEach(transactionsList::remove);
        Assert.assertEquals(2, transactionsList.size());
        transactionsList.forEach(transaction -> Assert.assertTrue(transaction.isPrivate()));
    }

    @Test
    public void testGetEncryptedPrivateBlockTransactions() throws Exception {
        String secretPhrase = accounts.get(PRIVATE_TRANSACTION_SENDER);
        byte[] publicKey = Crypto.getPublicKey(secretPhrase);
        List<JSONTransaction> transactionsList = client.getEncryptedPrivateBlockchainTransactionsList(url, PRIVATE_BLOCK_HEIGHT, null, null, secretPhrase, Convert.toHexString(publicKey));
        checkList(transactionsList);
        Assert.assertEquals(4, transactionsList.size());
        transactionsList.forEach(transaction -> {
            Assert.assertEquals(PRIVATE_BLOCK_HEIGHT, transaction.getHeight());
            Assert.assertEquals(PRIVATE_TRANSACTION_SENDER, transaction.getSenderRS());
        });
        List<JSONTransaction> publicTransactions = client.getBlockTransactionsList(url, PRIVATE_BLOCK_HEIGHT);
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
        List<JSONTransaction> transactionsList1 = client.getEncryptedPrivateBlockchainTransactionsList(url, -1, 5L, 9L, secretPhrase,
                Convert.toHexString(publicKey));
        checkList(transactionsList1);
        Assert.assertEquals(5, transactionsList1.size());
        checkAddress(transactionsList1, PRIVATE_TRANSACTION_SENDER);

        List<JSONTransaction> transactionsList2 = client.getEncryptedPrivateBlockchainTransactionsList(url, -1, null, 10L, secretPhrase,
                Convert.toHexString(publicKey));
        checkList(transactionsList2);
        Assert.assertEquals(11, transactionsList2.size());
        Assert.assertTrue(transactionsList2.containsAll(transactionsList1));
        checkAddress(transactionsList2, PRIVATE_TRANSACTION_SENDER);

        List<JSONTransaction> transactionsList3 = client.getEncryptedPrivateBlockchainTransactionsList(url, -1, 5L, null, secretPhrase,
                Convert.toHexString(publicKey));
        checkList(transactionsList3);
        Assert.assertTrue(transactionsList3.size() > 5);
        Assert.assertTrue(transactionsList3.containsAll(transactionsList1));
        checkAddress(transactionsList3, PRIVATE_TRANSACTION_SENDER);
    }

    @Test
    public void testGetEncryptedPrivateBlockTransactionsWithPagination() throws Exception {
        List<JSONTransaction> transactionsList1 = client.getPrivateBlockchainTransactionsList(url, accounts.get(PRIVATE_TRANSACTION_SENDER), -1, 5L,
                9L);
        checkList(transactionsList1);
        Assert.assertEquals(5, transactionsList1.size());
        checkAddress(transactionsList1, PRIVATE_TRANSACTION_SENDER);

        List<JSONTransaction> transactionsList2 = client.getPrivateBlockchainTransactionsList(url, accounts.get(PRIVATE_TRANSACTION_SENDER), -1,
                null, 10L);
        checkList(transactionsList2);
        Assert.assertEquals(11, transactionsList2.size());
        Assert.assertTrue(transactionsList2.containsAll(transactionsList1));
        checkAddress(transactionsList2, PRIVATE_TRANSACTION_SENDER);

        List<JSONTransaction> transactionsList3 = client.getPrivateBlockchainTransactionsList(url, accounts.get(PRIVATE_TRANSACTION_SENDER), -1, 5L,
                null);
        checkList(transactionsList3);
        Assert.assertTrue(transactionsList3.size() > 5);
        Assert.assertTrue(transactionsList3.containsAll(transactionsList1));
        checkAddress(transactionsList3, PRIVATE_TRANSACTION_SENDER);
    }

    @Test
    public void testGetBlockWithPrivateTransactions() throws Exception {
        List<JSONTransaction> blockTransactionsList1 = client.getBlockTransactionsList(url, PRIVATE_BLOCK_HEIGHT);
        List<JSONTransaction> blockTransactionsList2 = client.getBlockTransactionsList(url, PRIVATE_BLOCK_HEIGHT);
        checkList(blockTransactionsList1);
        checkList(blockTransactionsList2);
        Assert.assertEquals(4, blockTransactionsList1.size());
        blockTransactionsList2.forEach(blockTransactionsList1::remove);
        blockTransactionsList1.forEach(tr1 ->
                blockTransactionsList2.forEach(tr2 -> {
                    if (tr2.getFullHash().equalsIgnoreCase(tr1.getFullHash())) {
                        Assert.assertFalse(tr1.getSenderRS().equalsIgnoreCase(tr2.getSenderRS()));
                        Assert.assertFalse(tr1.getRecipientRS().equalsIgnoreCase(tr2.getRecipientRS()));
                        Assert.assertNotSame(tr1.getAmountATM(), (tr2.getAmountATM()));
                    }
                }));
    }

    @Test
    public void testGetTransactionsWithPagination() throws Exception {
        List<JSONTransaction> accountTransactions = client.getAccountTransactions(url, PRIVATE_TRANSACTION_SENDER, 1, 15, -1, -100);
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
    public void testSendAnyTransaction() throws IOException, ParseException {
        HashMap<String, String> pars = new HashMap<>();
        pars.put("requestType", "sendMoney");
        pars.put("amountATM", String.valueOf(atm(1L)));
        pars.put("recipient", TestUtil.getRandomRS(accounts));

        client.sendTransaction(url, "test5", atm(1L), 100, pars);
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
        List<JSONTransaction> unconfirmedTransactions = client.getUnconfirmedTransactions(url, PRIVATE_TRANSACTION_SENDER, 0, 3);
        checkList(unconfirmedTransactions);
        Assert.assertEquals(4, unconfirmedTransactions.size());
        for (int i = 1; i <= unconfirmedTransactions.size(); i++) {
            JSONTransaction transaction = unconfirmedTransactions.get(i - 1);
            Assert.assertEquals(PRIVATE_TRANSACTION_SENDER, transaction.getSenderRS());
            Assert.assertEquals(PRIVATE_TRANSACTION_RECIPIENT, transaction.getRecipientRS());
            Assert.assertEquals(atm(i), transaction.getAmountATM());
            Assert.assertFalse(transaction.isPrivate());
        }
    }

    @Test
    public void testGetPrivateUnconfirmedTransactions() throws Exception {
        for (int i = 1; i <= 6; i++) {
            client.sendMoney(url, accounts.get(PRIVATE_TRANSACTION_SENDER), PRIVATE_TRANSACTION_RECIPIENT, atm(i), NodeClient.DEFAULT_FEE, NodeClient.DEFAULT_DEADLINE);
            TimeUnit.SECONDS.sleep(1);
            client.sendMoneyPrivateTransaction(url, accounts.get(PRIVATE_TRANSACTION_SENDER), PRIVATE_TRANSACTION_RECIPIENT, atm(i) * 2,
                    NodeClient.DEFAULT_FEE, NodeClient.DEFAULT_DEADLINE);
            TimeUnit.SECONDS.sleep(1);
        }
        TimeUnit.SECONDS.sleep(3);
        List<JSONTransaction> unconfirmedTransactions = client.getPrivateUnconfirmedTransactions(url, accounts.get(PRIVATE_TRANSACTION_SENDER), 0, 9);
        checkList(unconfirmedTransactions);
        Assert.assertEquals(10, unconfirmedTransactions.size());
        for (int i = 1; i <= unconfirmedTransactions.size(); i++) {
            JSONTransaction transaction = unconfirmedTransactions.get(i - 1);
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
        String accountRs = getRandomRS(accounts);
        List<LedgerEntry> accountLedger = client.getAccountLedger(url, accountRs, true, 0, 500);
        LedgerEntry expectedLedgerEntry = accountLedger.stream().filter(LedgerEntry::isPublic).findFirst().get();
        LedgerEntry actualLedgerEntry = client.getAccountLedgerEntry(url, expectedLedgerEntry.getLedgerId(), true);
        Assert.assertEquals(AccountLedger.LedgerEvent.ORDINARY_PAYMENT, actualLedgerEntry.getEventType());
        Assert.assertEquals(expectedLedgerEntry, actualLedgerEntry);
    }

    @Test
    public void testGetUnknownAccountLedgerEntry() throws Exception {
        LedgerEntry ledgerEntry = client.getAccountLedgerEntry(url, 0L, true);
        Assert.assertTrue(ledgerEntry.isNull());
    }

    @Test
    public void testGetPrivateAccountLedgerEntry() throws Exception {
        String accountRs = getRandomRS(accounts);
        List<LedgerEntry> accountLedger = client.getPrivateAccountLedger(url, accounts.get(accountRs), true, 0, 500);
        LedgerEntry expectedPrivateLedgerEntry = accountLedger.stream().filter(LedgerEntry::isPrivate).findFirst().get();
        LedgerEntry actualLedgerEntry = client.getPrivateAccountLedgerEntry(url, accounts.get(accountRs), expectedPrivateLedgerEntry.getLedgerId(), true);
        Assert.assertFalse(actualLedgerEntry.isNull());
        Assert.assertEquals(AccountLedger.LedgerEvent.PRIVATE_PAYMENT, actualLedgerEntry.getEventType());
        Assert.assertEquals(expectedPrivateLedgerEntry, actualLedgerEntry);
    }

    @Test
    public void testGetTransactionsWithType() throws IOException {
        List<JSONTransaction> transactions = client.getAccountTransactions(url, PRIVATE_TRANSACTION_SENDER, 0, 99, 0, -1);
        checkList(transactions);
        Assert.assertEquals(100, transactions.size());
        transactions.forEach(transaction -> {
            Assert.assertFalse(transaction.isPrivate());
            Assert.assertEquals(0, transaction.getType().getType());
            if (!transaction.isOwnedBy(PRIVATE_TRANSACTION_SENDER)) {
                Assert.fail("Not this user: " + PRIVATE_TRANSACTION_SENDER + ", transaction: " + transaction.toString());
            }
        });
    }

    @Test
    public void testGetTransactionsWithSubtype() throws IOException {
        List<JSONTransaction> transactions = client.getAccountTransactions(url, PRIVATE_TRANSACTION_SENDER, 0, 49, -71, 1);
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
        List<JSONTransaction> transactions = client.getAccountTransactions(url, PRIVATE_TRANSACTION_SENDER, 0, 49, 0, 0);
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
        List<JSONTransaction> transactions1 = client.getAccountTransactions(url, PRIVATE_TRANSACTION_SENDER, 0, 49, 0, 1);
        Assert.assertTrue("List contains Private transactions!", transactions1.isEmpty());
        //do request for each type
        for (int i = 0; i < 20; i++) {
            List<JSONTransaction> transactions2 = client.getAccountTransactions(url, PRIVATE_TRANSACTION_SENDER, 0, 49, i, -1);
            int type = i;
            transactions2.forEach(transaction -> {
                Assert.assertTrue("Not this user: " + PRIVATE_TRANSACTION_SENDER + " transaction: " + transaction, transaction.isOwnedBy(PRIVATE_TRANSACTION_SENDER));
                Assert.assertFalse("Transaction is private! " + transaction + ". Type: " + type, transaction.isPrivate());
            });
            //do request for each subtype
            for (int j = 0; j < 20; j++) {
                List<JSONTransaction> transactions3 = client.getAccountTransactions(url, PRIVATE_TRANSACTION_SENDER, 0, 49, i, j);
                int subtype = j;
                transactions3.forEach(transaction -> {
                    Assert.assertTrue("Not this user: " + PRIVATE_TRANSACTION_SENDER + " transaction: " + transaction, transaction.isOwnedBy(PRIVATE_TRANSACTION_SENDER));
                    Assert.assertFalse("Transaction is private! " + transaction + ". Type: " + type + ". Subtype: " + subtype, transaction.isPrivate());
                });
            }
        }
    }

    @Test
    @Ignore
    public void testSendUpdateTransaction() throws IOException, ParseException {
        Assert.fail("Encrypted url is required");
        String firstPart = "a427a6d86645d0c32527e50fe292a0b1cf3983ef083f9fc392359e34d90012a65d5bd927c2cd09466433c107e523ff01bc00e414108d01e515f56ddbc054abce83fa4bd30bdf4623928e768536f8e56d9695ebadfbe34b5d1d59aa63545f5238a4817ec09389687df5ec116423b0e572a5ee9c47eaab432b19805a610beecb495595636a14009524caee8f1c73db084f1842bf895440233bff67c8f09674056113efd58da69f8411df3df174438bd2e8280e4eac97d6f89a6d756c1feddccc6d593d59578aab46ad9024b0ba742c547418ea7b2adbed80c8f673cd2cff31fefb6ab068c03232d79dfd83977a05bb0fb286f81ddbc0a9c75e6fce81747223a8fe5e506f9a9d7a7fd08d51b63ba25b4872886857b59607e24e842aa39e9d0d78a3db3ad97b03e64fb135ef55f5f396e29c8a4e146087b853f9a1be0a647201836da32ef5b0bff1a3bc599bff155cbfe8a24ad5ee7ab711bf9de7682876c8b7986025e68c8ee63f63505d3ec21f53a98e9de78f39b69c8438028a0e569f81c9ac7bc7d2dc0ea4f4406a696938fe422bad1076342267ee13d657aa9e68d07aafba6b33fc3e90d72ea5147bc21d223b862c56d989a568a7a2609b272261df3af318f340283490ff4d909768deee8987e363bba10c489d746e4e706daf02b78ba5886f59c204bc2237702d1c2191a6c6b0d3095c9c3d462e4e1cae02f0f53b5e94c215";
        String secondPart =
                "b51c553a2e69bc868926235c2fc01ba04b69070324a0c94d9c0d32f65ad4bb475c2b2887800caed2f4023f6510c363a5c4a7da0d8ba7cf85e921990fa7eba87c053ee753157c7541b291483a3f444b0e5d91dcb0f74def9dbe46c910546d0b616ebd9241e7f09aa619cb84b95560307d7e6b07e4fa47c508a621683717485542883203f1f17279b5e93173fa01b19bc707b1ee899bd1118322befed65b6eb28df579d56e61ca6b90abe5408f21544e3e6195ab23876baab07db967de04e815a9395987775acbe57bb7ac8d7366ad62a655bb4598edb4d3d2dce3d326fbeef97b654c686e9abd2c613ea740701a5a4d647e1ebf3bda0fc29fdbb5dfc7dc22842f32e552b0f999076d5f644809ff752224b71fe2f85ad8ac4766d57756d52953bbfb6e6b2134b173bf4995218429371ce3989cd764482396acb05eeaf2e138f38bae9107a9b6db626c6647be5d4a1e6f02f17326700ddeec0b8037671252f0e5c475e06964b6c5a5ff51bc07b494ee84ef5be7d84146f949fe6639409c3fe7550597e45c93ec276721781d9e8677fe4501b583a2b6d96d583c6397c8c5ef14ab6932581d81a8a3518da882fb920dd47c4af25ed755697a7cb181936ae0f21f3c2976f3168202e02fc4b351dcbb7f0c9e5b50a7f1f1d1841dd4de09ca374e3d01fc4fa6cb9271c727a194a2b701ec5e7d882790bb800cc2f86339ad708869ea2911";
        Assert.assertEquals(1024, firstPart.length());
        Assert.assertEquals(1024, secondPart.length());

        DoubleByteArrayTuple updateUrl = new DoubleByteArrayTuple(
                Convert.parseHexString(firstPart), Convert.parseHexString(secondPart));

        String hash = "92d5e38b0a3d73d5ce36adc3df998145a070e2b4924cf48aa7898822320bdd0b";
        Platform platform = Platform.WINDOWS;
        Architecture architecture = Architecture.AMD64;
        Version version = Version.from("1.0.8");
        JSONTransaction updateTransaction = client.sendUpdateTransaction(url, accounts.get(PRIVATE_TRANSACTION_SENDER), 100_000_000, 0, updateUrl,
                version, architecture, platform, hash, 5);
        Attachment.UpdateAttachment expectedAttachment = Attachment.UpdateAttachment.getAttachment(platform, architecture, updateUrl, version,
                Convert.parseHexString(hash)
                , (byte) 0);
        Assert.assertEquals(TransactionType.Update.CRITICAL, updateTransaction.getType());
        Assert.assertNull(updateTransaction.getRecipientRS());
        Assert.assertEquals(100_000_000, updateTransaction.getFeeATM());
        Assert.assertEquals(expectedAttachment, updateTransaction.getAttachment());
    }

    @Test
    public void testGetAccountLedgerWithoutParameters() throws Exception {
        List<LedgerEntry> accountLedger = client.getAccountLedger(url, "", false, 0, 4);
        checkList(accountLedger);
        Assert.assertEquals(5, accountLedger.size());
        accountLedger.forEach(LedgerEntry::isPublic);
    }

    @Test
    public void testGetAccounts() throws IOException {
        List<Account> allAccounts = client.getAllAccounts(url, 0, 14);
        Assert.assertEquals(15, allAccounts.size());
        List<Account> sortedAccounts = allAccounts.stream().sorted(Comparator.comparingLong(Account::getBalanceATM).reversed()).collect(Collectors.toList());
        Assert.assertEquals(sortedAccounts, allAccounts);
        long minBal = allAccounts.get(14).getBalanceATM();
        Assert.assertTrue(minBal >= client.getAllAccounts(url, 15, 15).get(0).getBalanceATM());
    }
}



