/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import static com.apollocurrency.aplwallet.apl.TestConstants.TEST_LOCALHOST;
import static org.slf4j.LoggerFactory.getLogger;
import static util.TestUtil.atm;

import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Convert;
import dto.JSONTransaction;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class TestDataGenerator {
        private static final Logger LOG = getLogger(TestDataGenerator.class);

    private static NodeClient client = new NodeClient();

    public static Map<TestAccount, List<JSONTransaction>> generateChatsForAccount(TestAccount acc) throws Exception {
        Map<TestAccount, List<JSONTransaction>> chatAccounts = new HashMap<>();
        TestAccount randomAcc1 = generateAccount("randomAcc1_");
        TestAccount randomAcc2 = generateAccount("randomAcc2_");
        TestAccount randomAcc3 = generateAccount("randomAcc3_");
        Thread acc1Thread = new Thread(() -> {
            try {
                fundAcc(randomAcc1, acc, 10L);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }, "randomAcc1 chat generation");
        Thread acc2Thread = new Thread(() -> {
            try {
                fundAcc(randomAcc2, acc, 10L);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }, "randomAcc2 chat generation");
        Thread acc3Thread = new Thread(() -> {
            try {
                fundAcc(randomAcc3, acc, 10L);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }, "randomAcc3 chat generation");
        acc1Thread.start();
        acc2Thread.start();
        acc3Thread.start();
        acc1Thread.join();
        acc2Thread.join();
        acc3Thread.join();
        List<JSONTransaction> unconfirmedTransctions1 = generateChatTransactions(acc, randomAcc1, 2, 2);
        List<JSONTransaction> unconfirmedTransactions2 = generateChatTransactions(acc, randomAcc2, 3, 4);
        List<JSONTransaction> unconfirmedTransactions3 = generateChatTransactions(acc, randomAcc3, 0, 1);
        LOG.debug("Waiting confirmations for chat transactions");
        chatAccounts.put(randomAcc1, waitForConfirmation(unconfirmedTransctions1));
        chatAccounts.put(randomAcc2, waitForConfirmation(unconfirmedTransactions2));
        chatAccounts.put(randomAcc3, waitForConfirmation(unconfirmedTransactions3));
        return chatAccounts;
    }

    public static JSONTransaction generateChatTransaction(TestAccount sender, TestAccount recipient) throws IOException, ParseException {
        return client.sendChatTransaction(TEST_LOCALHOST, "Test message", sender.getSecretPhrase(), 60, recipient.getAccountRS(), atm(1));
    }

    public static List<JSONTransaction> generateChatTransactions(TestAccount sender, TestAccount recipient, int numberOfReceivedTransactions,
                                                                 int numberOfSentTransactions) throws Exception {
        List<JSONTransaction> transactions = new ArrayList<>();
        LOG.debug("Generating chat transaction between accounts: {} and {}", sender.getAccountRS(), recipient.getAccountRS());
        for (int i = 0; i < Math.min(numberOfReceivedTransactions, numberOfSentTransactions); i++) {
            transactions.add(generateChatTransaction(sender, recipient));
            TimeUnit.SECONDS.sleep(1);
            JSONTransaction tr = generateChatTransaction(recipient, sender);
            transactions.add(tr);
            TimeUnit.SECONDS.sleep(1);
        }
        TestAccount s = numberOfReceivedTransactions > numberOfSentTransactions ? recipient : sender;
        TestAccount r = numberOfReceivedTransactions > numberOfSentTransactions ? sender : recipient;
        for (int i = Math.min(numberOfReceivedTransactions, numberOfSentTransactions); i < Math.max(numberOfReceivedTransactions,
                numberOfSentTransactions); i++) {
            JSONTransaction tr = generateChatTransaction(s, r);
            transactions.add(tr);
            TimeUnit.SECONDS.sleep(1);
        }

        transactions.sort(Comparator.comparingLong(JSONTransaction::getTimestamp).reversed());
        return transactions;
    }

    public static void fundAcc(TestAccount account, TestAccount fundingAcc, long amount) throws Exception {
        JSONTransaction jsonTransaction = client.sendMoneyTransaction(TEST_LOCALHOST, fundingAcc.getSecretPhrase(),
                Convert.toHexString(Crypto.getPublicKey(account.getSecretPhrase())),60,
                account.getAccountRS(), atm(amount), atm(1L));
        LOG.debug("Funding acc: {} -> {}", fundingAcc.getAccountRS(), account.getAccountRS());
        waitForConfirmation(jsonTransaction);

    }

    private static boolean waitForConfirmation(JSONTransaction transaction) throws Exception {
        while (true) {
            JSONTransaction receivedTransaction = client.getTransaction(TEST_LOCALHOST, transaction.getFullHash());
            if (receivedTransaction != null && receivedTransaction.getNumberOfConfirmations() >= 0) {
                return true;
            }
            TimeUnit.SECONDS.sleep(1);
        }
    }

    private static List<JSONTransaction> waitForConfirmation(List<JSONTransaction> transactions) {
        List<JSONTransaction> confirmedTransactions = new ArrayList<>();
        for (JSONTransaction tr : transactions) {
            while (true) {
                try {
                    JSONTransaction receivedTransaction = client.getTransaction(TEST_LOCALHOST, tr.getFullHash());
                    TimeUnit.SECONDS.sleep(1);
                    if (receivedTransaction != null && receivedTransaction.getNumberOfConfirmations() >= 0) {
                        confirmedTransactions.add(receivedTransaction);
                        break;
                    }
                }
                catch (Exception e) {
                    throw new RuntimeException(e.toString(), e);
                }
            }
        }
        confirmedTransactions.sort(Comparator.comparingLong(JSONTransaction::getTimestamp).reversed());
        return confirmedTransactions;
    }
    public static TestAccount generateAccount(String namePrefix) {
        String secretPhrase = generateSecretPhrase();
        byte[] publicKey = Crypto.getPublicKey(secretPhrase);
        long id = Account.getId(publicKey);
        String name = namePrefix + Crypto.getSecureRandom().nextInt();
        return new TestAccount(id, secretPhrase);
    }
    public static JSONTransaction saveAcc(TestAccount account) throws Exception {
            JSONTransaction jsonTransaction = client.setAccountInfo(TEST_LOCALHOST, "acc" + new Random().nextInt(100), account.getSecretPhrase(), 60,
                    atm(1));
            waitForConfirmation(jsonTransaction);
        return jsonTransaction;
    }

    public static String generateSecretPhrase() {
        return "test" + new Random().nextInt();
    }
}
