package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.PollDTO;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseOld;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Epic;
import org.apache.commons.lang3.RandomStringUtils;
import org.bouncycastle.crypto.prng.RandomGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
@DisplayName("Poll")
@Epic(value = "Poll")
public class TestPoll extends TestBaseOld {
    private final ArrayList<Wallet> wallets = new ArrayList<>();
    @BeforeEach
    @Override
    public void setUP(TestInfo testInfo) {
        super.setUP(testInfo);
        wallets.add(TestConfiguration.getTestConfiguration().getStandartWallet());
        wallets.add(TestConfiguration.getTestConfiguration().getVaultWallet());
    }

    @DisplayName("Creating Poll")
    @ParameterizedTest(name = "{displayName} votingModel: {0}")
    @ValueSource(ints = { 0,1,2,3 })
    public void createPollTest(int votingModel) {
        for (Wallet wallet: wallets) {
            String name = RandomStringUtils.randomAlphabetic(7);
            int plusFinishHeight = 15;
            CreateTransactionResponse poll = createPoll(wallet, votingModel, name, plusFinishHeight, "");
            assertNotNull(poll);
            verifyCreatingTransaction(poll);
            verifyTransactionInBlock(poll.getTransaction());
            int currentHeight = getBlock().getHeight();
            String pollId = poll.getTransaction();
            assertNotNull(getPoll(pollId));
            assertEquals(name, getPoll(pollId).getName(), "Names of poll are different");
            assertEquals(false, getPoll(pollId).getFinished(), "status of poll is different");
            waitForHeight(currentHeight + plusFinishHeight + 1);
            assertEquals(true, getPoll(pollId).getFinished(), "status of finished poll is different");
        }
    }

}
