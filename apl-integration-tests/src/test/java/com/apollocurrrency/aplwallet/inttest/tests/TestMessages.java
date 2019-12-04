package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.AccountMessageDTO;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrrency.aplwallet.inttest.helper.WalletProvider;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseOld;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Messages")
@Epic(value = "Messages")
public class TestMessages extends TestBaseOld {

    @DisplayName("Send Message/Read Message")
    @Feature(value = "Not Private Message")
    @Story(value = "Send Message")
    @ParameterizedTest(name = "{displayName} Wallet type: {0}")
    @ArgumentsSource(WalletProvider.class)
    public void readMessage(Wallet wallet) throws IOException {
        String textMessage = "Test MSG";
        CreateTransactionResponse response = sendMessage(wallet, wallet.getUser(), textMessage);
        verifyCreatingTransaction(response);
        verifyTransactionInBlock(response.getTransaction());
        AccountMessageDTO message = readMessage(wallet, response.getTransaction());
        assertEquals(textMessage, message.getMessage());
    }

    @DisplayName("Send Message Phasing")
    @Feature(value = "Not Private Message ")
    @Story(value = "Send Message")
    @ParameterizedTest(name = "{displayName} Wallet type: {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void readMessagePhasing(Wallet wallet) throws IOException {
        String textMessage = "Test MSG Phasing";
        CreateTransactionResponse response = sendMessage(wallet, wallet.getUser(), textMessage);
        verifyCreatingTransaction(response);
        verifyTransactionInBlock(response.getTransaction());
        AccountMessageDTO message = readMessage(wallet, response.getTransaction());
        assertEquals(textMessage, message.getMessage());
    }

    @DisplayName("Send Prunable Message")
    @Feature(value = "Not Private Message")
    @Story(value = "Send Message")
    @ParameterizedTest(name = "{displayName} Wallet type: {0}")
    @ArgumentsSource(WalletProvider.class)
    public void messageAttachmentTest(Wallet wallet) throws IOException {
        String textMessage = "Test MSG";
        messagePrunable();
        CreateTransactionResponse response = sendMessage(wallet, wallet.getUser(), textMessage);
        verifyCreatingTransaction(response);
        verifyTransactionInBlock(response.getTransaction());
        AccountMessageDTO message = readMessage(wallet, response.getTransaction());
        assertEquals(textMessage, message.getMessage());
    }
}
