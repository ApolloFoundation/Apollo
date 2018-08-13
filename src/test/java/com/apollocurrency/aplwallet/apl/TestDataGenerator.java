/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.apollocurrency.aplwallet.apl.TestData.TEST_LOCALHOST;
import static org.slf4j.LoggerFactory.getLogger;
import static util.TestUtil.atm;

public class TestDataGenerator {
        private static final Logger LOG = getLogger(TestDataGenerator.class);

    private static NodeClient client = new NodeClient();

    public static Map<TestAccount, List<JSONTransaction>> generateChatsForAccount(TestAccount acc) throws Exception {
        Map<TestAccount, List<JSONTransaction>> chatAccounts = new HashMap<>();

        TestAccount randomAcc1 = generateAccount("randomAcc1_", true);
        fundAcc(randomAcc1, acc, 10L);
        chatAccounts.put(randomAcc1, generateChatTransactions(acc, randomAcc1, 2, 2, false));

        TestAccount randomAcc2 = generateAccount("randomAcc2_", true);
        fundAcc(randomAcc2, acc, 10L);
        chatAccounts.put(randomAcc2, generateChatTransactions(acc, randomAcc2, 3, 4, false));

        TestAccount randomAcc3 = generateAccount("randomAcc3_", true);
        fundAcc(randomAcc3, acc, 10L);
        chatAccounts.put(randomAcc3, generateChatTransactions(acc, randomAcc3, 0, 1, true));

        return chatAccounts;
    }

    public static JSONTransaction generateChatTransaction(TestAccount sender, TestAccount recipient) throws IOException, ParseException {
        return client.sendChatTransaction(TEST_LOCALHOST, "Test message", sender.getSecretPhrase(), 60, Convert.rsAccount(recipient.getId()), atm(1));
    }

    public static List<JSONTransaction> generateChatTransactions(TestAccount sender, TestAccount recipient, int numberOfReceivedTransactions,
                                                                 int numberOfSentTransactions, boolean wait) throws Exception {
        List<JSONTransaction> transactions = new ArrayList<>();
        LOG.debug("Generating chat transaction for account: {} and {}", sender.getRS(), recipient.getRS());
        for (int i = 0; i < Math.min(numberOfReceivedTransactions, numberOfSentTransactions); i++) {
            transactions.add(generateChatTransaction(sender, recipient));
            JSONTransaction tr = generateChatTransaction(recipient, sender);
            transactions.add(tr);
        }
        TestAccount s = numberOfReceivedTransactions > numberOfSentTransactions ? recipient : sender;
        TestAccount r = numberOfReceivedTransactions > numberOfSentTransactions ? sender : recipient;
        for (int i = Math.min(numberOfReceivedTransactions, numberOfSentTransactions); i < Math.max(numberOfReceivedTransactions,
                numberOfSentTransactions); i++) {
            JSONTransaction tr = generateChatTransaction(s, r);
            transactions.add(tr);
        }
        if (wait) {
            LOG.debug("Waiting confirmations for chat transactions");
            waitForConfirmation(transactions, 100);
        }
        return transactions;
    }

    public static void fundAcc(TestAccount account, TestAccount fundingAcc, long amount) throws Exception {
        JSONTransaction jsonTransaction = client.sendMoneyTransaction(TEST_LOCALHOST, fundingAcc.getSecretPhrase(),
                Convert.toHexString(account.getPublicKey()),60,
                Convert.rsAccount(account.getId()), atm(amount), atm(1L));
        LOG.debug("Funding acc: {} to {}", fundingAcc.getRS(), account.getRS());
        waitForConfirmation(jsonTransaction, 100);

    }

    private static boolean waitForConfirmation(JSONTransaction transaction, int seconds) throws Exception {
        while (seconds > 0) {
            seconds -= 1;
            JSONTransaction receivedTransaction = client.getTransaction(TEST_LOCALHOST, transaction.getFullHash());
            if (receivedTransaction != null && receivedTransaction.getNumberOfConfirmations() > 0) {
                return true;
            }
            TimeUnit.SECONDS.sleep(1);
        }
        return false;
    }

    private static boolean waitForConfirmation(List<JSONTransaction> transactions, int seconds) {
        for (int i = 0; i < transactions.size(); i++) {
            JSONTransaction tr = transactions.get(i);
            int remaining = seconds;
            while (remaining > 0) {
                remaining -= 1;
                try {
                JSONTransaction receivedTransaction = client.getTransaction(TEST_LOCALHOST, tr.getFullHash());
                    TimeUnit.SECONDS.sleep(1);
                if (receivedTransaction != null && receivedTransaction.getNumberOfConfirmations() > 0) {
                    return true;
                }
                }
                catch (Exception e) {
                    throw new RuntimeException(e.toString(), e);
                }
            }
        }
        return false;
    }


    public static TestAccount generateAccount(String namePrefix, boolean saveName) throws Exception {
        String secretPhrase = generateSecretPhrase();
        byte[] publicKey = Crypto.getPublicKey(secretPhrase);
        long id = Account.getId(publicKey);
        String name = namePrefix + Crypto.getSecureRandom().nextInt();
        if (saveName) {
            JSONTransaction jsonTransaction = client.setAccountInfo(TEST_LOCALHOST, name, secretPhrase, 60, atm(1));
            waitForConfirmation(jsonTransaction, 100);
        }
        return new TestAccount(id, publicKey, name, secretPhrase);
    }

    public static String generateSecretPhrase() {
        return "test" + new Random().nextInt();
    }
}
