package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.PollDTO;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseOld;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Epic;
import org.apache.commons.lang3.ObjectUtils;
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

    private final int POLL_BY_ACCOUNT = 0;
    private final int POLL_BY_ACCOUNT_BALANCE = 1;
    private final int POLL_BY_ASSET = 2;
    private final int POLL_BY_CURRENCY = 3;

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

        String name = RandomStringUtils.randomAlphabetic(7);
        int plusFinishHeight = 9;
        CreateTransactionResponse poll = null;
        CreateTransactionResponse vote = null;
        int currentHeight;
        String pollId;

        for (Wallet wallet: wallets) {
            switch (votingModel) {
                case POLL_BY_ACCOUNT:
                    poll = createPoll(wallet, votingModel, name, plusFinishHeight, "", 0);
                    break;
                case POLL_BY_ACCOUNT_BALANCE:
                    //TODO: add balance verification
                    poll = createPoll(wallet, votingModel, name, plusFinishHeight, "", 10000);
                    break;
                case POLL_BY_ASSET:
                    CreateTransactionResponse asset = issueAsset(wallet, RandomStringUtils.randomAlphabetic(5), "description of asset for poll", 100);
                    verifyCreatingTransaction(asset);
                    verifyTransactionInBlock(asset.getTransaction());
                    String assetId = asset.getTransaction();
                    poll = createPoll(wallet, votingModel, name, plusFinishHeight, assetId, 1);
                    break;
                case POLL_BY_CURRENCY:
                    CreateTransactionResponse currency = issueCurrency(wallet, 1, RandomStringUtils.randomAlphabetic(5), "description of currency for poll", RandomStringUtils.randomAlphabetic(4).toUpperCase(), 100, 100,1);
                    verifyCreatingTransaction(currency);
                    verifyTransactionInBlock(currency.getTransaction());
                    String currencyId = currency.getTransaction();
                    poll = createPoll(wallet, votingModel, name, plusFinishHeight, currencyId, 1);
                    break;
            }
            assertNotNull(poll);
            verifyCreatingTransaction(poll);
            verifyTransactionInBlock(poll.getTransaction());
            currentHeight = getBlock().getHeight();
            pollId = poll.getTransaction();
            assertEquals(name, getPoll(pollId).getName(), "Names of poll are different");
            assertEquals(false, getPoll(pollId).getFinished(), "status of poll is different");
            vote = castVote(wallet, pollId);
            verifyCreatingTransaction(vote);
            verifyTransactionInBlock(vote.getTransaction());
            waitForHeight(currentHeight + plusFinishHeight + 1);
            assertEquals(true, getPoll(pollId).getFinished(), "status of finished poll is different");
        }

    }

}

