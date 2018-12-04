/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import static com.apollocurrency.aplwallet.apl.TestConstants.TEST_FILE;
import static com.apollocurrency.aplwallet.apl.TestConstants.TEST_LOCALHOST;
import static util.TestUtil.checkList;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.apollocurrency.aplwallet.apl.TestAccount;
import com.apollocurrency.aplwallet.apl.TestDataGenerator;
import com.apollocurrency.aplwallet.apl.util.Convert;
import dto.ChatInfo;
import dto.JSONTransaction;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import util.TestUtil;

public class ChatTest extends APITest {
    public static TestAccount randomChatAccount;
    private static TestAccount chatAcc;
    private static Map<TestAccount, List<JSONTransaction>> chats;
    public ChatTest() {

    }


    @BeforeClass
    public static void setUp() throws Exception {
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

    @Test
    public void testGetChats() throws IOException {
        List<ChatInfo> chatInfo = nodeClient.getChatInfo(TEST_LOCALHOST, chatAcc.getAccountRS());
//        List<Chat.ChatInfo> chatInfo = client.getChatInfo(url, "APL-NFCE-2L6E-6NKP-8FH59");
        checkList(chatInfo);
        Assert.assertEquals(chats.size(), chatInfo.size());
        chatInfo.forEach(c -> {
            List<TestAccount> matchedAcc =
                    chats.keySet().stream().filter(acc -> c.getAccount().equals(acc)).collect(Collectors.toList());
            Assert.assertNotNull(matchedAcc);
            Assert.assertEquals(1, matchedAcc.size());
            Optional<JSONTransaction> first = chats.get(matchedAcc.get(0)).stream().max(Comparator.comparingLong(JSONTransaction::getTimestamp));
            Assert.assertTrue(first.isPresent());
            Assert.assertEquals(c.getLastMessageTime(), first.get().getTimestamp());
        });
    }

    @Test
    public void testGetChatHistory() throws IOException {
        List<JSONTransaction> chatTransactions = nodeClient.getChatHistory(TEST_LOCALHOST, chatAcc.getAccountRS(), randomChatAccount.getAccountRS(), 0, 5);
        checkList(chatTransactions);
        Assert.assertEquals(chats.get(randomChatAccount).size() - 1, chatTransactions.size());
        for (int i = 0; i < chats.get(randomChatAccount).subList(0, 6).size(); i++) {
            JSONTransaction expected = chats.get(randomChatAccount).get(i);
            JSONTransaction actual = chatTransactions.get(i);
            Assert.assertEquals(expected.getHeight(), actual.getHeight());
            Assert.assertEquals(expected.getTimestamp(), actual.getTimestamp());
            Assert.assertEquals(expected.getSender(), actual.getSender());
            Assert.assertEquals(expected.getRecipient(), actual.getRecipient());
            Assert.assertEquals(Convert.toString(expected.getMessage().getMessage(), true), Convert.toString(actual.getMessage().getMessage(), true));
        }
    }
}
