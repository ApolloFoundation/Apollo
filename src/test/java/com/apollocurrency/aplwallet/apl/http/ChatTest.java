/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.*;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.junit.*;
import util.TestUtil;
import util.WalletRunner;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.apollocurrency.aplwallet.apl.TestData.TEST_FILE;
import static com.apollocurrency.aplwallet.apl.TestData.TEST_LOCALHOST;
import static util.TestUtil.checkList;
@Ignore
public class ChatTest {
    public static TestAccount randomChatAccount;
    private static TestAccount chatAcc;
    private static Map<TestAccount, List<JSONTransaction>> chats;
    private NodeClient client = new NodeClient();
    public ChatTest() {

    }

    private static WalletRunner runner = new WalletRunner();

    @BeforeClass
    public static void setUp() throws Exception {
        runner.run();
        try {
            chatAcc = TestDataGenerator.generateAccount("chat_acc");
            TestDataGenerator.fundAcc(chatAcc, new TestAccount(TestUtil.getRandomSecretPhrase(TestUtil.loadKeys(TEST_FILE))), 50);
            chats = TestDataGenerator.generateChatsForAccount(chatAcc);
            chats.forEach((acc, trs) -> {
                if (trs.size() == 7) {
                    randomChatAccount = acc;
                }
            });
        }
        catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        runner.shutdown();
    }
    @Test
    public void testGetChats() throws IOException {
        List<Chat.ChatInfo> chatInfo = client.getChatInfo(TEST_LOCALHOST, chatAcc.getRS());
//        List<Chat.ChatInfo> chatInfo = client.getChatInfo(url, "APL-NFCE-2L6E-6NKP-8FH59");
        checkList(chatInfo);
        Assert.assertEquals(chats.size(), chatInfo.size());
        chatInfo.forEach(c -> {
            List<TestAccount> matchedAcc = chats.keySet().stream().filter(acc -> c.getAccount() == acc.getId()).collect(Collectors.toList());
            Assert.assertNotNull(matchedAcc);
            Assert.assertEquals(1, matchedAcc.size());
            Optional<JSONTransaction> first = chats.get(matchedAcc.get(0)).stream().max(Comparator.comparingLong(JSONTransaction::getTimestamp));
            Assert.assertTrue(first.isPresent());
            Assert.assertEquals(c.getLastMessageTime(), first.get().getTimestamp());
        });
    }

    @Test
    public void testGetChatHistory() throws IOException {
        List<JSONTransaction> chatTransactions = client.getChatHistory(TEST_LOCALHOST, chatAcc.getRS(), randomChatAccount.getRS(), 0, 5);
        checkList(chatTransactions);
        Assert.assertEquals(chats.get(randomChatAccount).size() - 1, chatTransactions.size());
        for (int i = 0; i < chats.get(randomChatAccount).subList(0, 6).size(); i++) {
            JSONTransaction expected = chats.get(randomChatAccount).get(i);
            JSONTransaction actual = chatTransactions.get(i);
            Assert.assertEquals(expected.getHeight(), actual.getHeight());
            Assert.assertEquals(expected.getTimestamp(), actual.getTimestamp());
            Assert.assertEquals(expected.getSenderRS(), actual.getSenderRS());
            Assert.assertEquals(expected.getRecipientRS(), actual.getRecipientRS());
            Assert.assertEquals(Convert.toString(expected.getMessage().getMessage(), true), Convert.toString(actual.getMessage().getMessage(), true));
        }
    }
}
