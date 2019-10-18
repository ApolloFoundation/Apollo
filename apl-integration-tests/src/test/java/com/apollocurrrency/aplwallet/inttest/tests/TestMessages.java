package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrrency.aplwallet.inttest.helper.WalletProvider;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseOld;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import com.apollocurrency.aplwallet.api.dto.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class TestMessages extends TestBaseOld {


    @DisplayName("Send Message/Read Message")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void readMessage(Wallet wallet) throws IOException {
        String textMessage = "Test MSG";
        CreateTransactionResponse response = sendMessage(wallet,wallet.getUser(),textMessage);
        verifyCreatingTransaction(response);
        verifyTransactionInBlock(response.getTransaction());
        AccountMessageDTO message =  readMessage(wallet,response.getTransaction());
        assertEquals(textMessage,message.getMessage());
    }

    @DisplayName("Send Message Phasing")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void readMessagePhasing(Wallet wallet) throws IOException {
        String textMessage = "Test MSG Phasing";
       // createPhasingVote(VoteWeighting.VotingModel.ATM,"136524",1, 1,0,"0");
        CreateTransactionResponse response = sendMessage(wallet,wallet.getUser(),textMessage);
        verifyCreatingTransaction(response);
        verifyTransactionInBlock(response.getTransaction());
        AccountMessageDTO message =  readMessage(wallet,response.getTransaction());
        assertEquals(textMessage,message.getMessage());
    }
}
