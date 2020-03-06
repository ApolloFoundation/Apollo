package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.helper.providers.PollArgumentProvider;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseNew;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Epic;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Poll")
@Epic(value = "Poll")
public class TestPoll extends TestBaseNew {

    private final int POLL_BY_ACCOUNT = 0;
    private final int POLL_BY_ACCOUNT_BALANCE = 1;
    private final int POLL_BY_ASSET = 2;
    private final int POLL_BY_CURRENCY = 3;

    private final ArrayList<Wallet> wallets = new ArrayList<>();

    @BeforeEach
    @Override
    public void setUp(TestInfo testInfo) {
        super.setUp(testInfo);
        wallets.add(TestConfiguration.getTestConfiguration().getStandartWallet());
        wallets.add(TestConfiguration.getTestConfiguration().getVaultWallet());
    }

    @DisplayName("Poll flow. Creating. Voting.")
    @ParameterizedTest(name = "{displayName} votingModel: {0} for: {1}")
    @ArgumentsSource(PollArgumentProvider.class)
    public void pollTest(int votingModel,Wallet wallet) {

        String name = RandomStringUtils.randomAlphabetic(7);
        int plusFinishHeight = 9;
        CreateTransactionResponse poll = null;
        CreateTransactionResponse vote = null;
        int currentHeight;
        String pollId;
        int maxRangeValue = 10;
        int voteNumber = RandomUtils.nextInt(1, maxRangeValue);
        long weight = 1;
        long result = 0;

            switch (votingModel) {
                case POLL_BY_ACCOUNT:
                    poll = createPoll(wallet, votingModel, name, plusFinishHeight, "", 0, maxRangeValue);
                    break;
                case POLL_BY_ACCOUNT_BALANCE:
                    poll = createPoll(wallet, votingModel, name, plusFinishHeight, "", 10000, maxRangeValue);
                    break;
                case POLL_BY_ASSET:
                    int quantityATU = 100;
                    weight = quantityATU;
                    CreateTransactionResponse asset = issueAsset(wallet, RandomStringUtils.randomAlphabetic(5), "description of asset for poll", quantityATU);
                    verifyCreatingTransaction(asset);
                    verifyTransactionInBlock(asset.getTransaction());
                    poll = createPoll(wallet, votingModel, name, plusFinishHeight, asset.getTransaction(), 1, maxRangeValue);
                    break;
                case POLL_BY_CURRENCY:
                    int initialSupply = 100;
                    weight = initialSupply;
                    CreateTransactionResponse currency = issueCurrency(wallet, 1, RandomStringUtils.randomAlphabetic(5), "description of currency for poll", RandomStringUtils.randomAlphabetic(4).toUpperCase(), initialSupply, 100, 1);
                    verifyCreatingTransaction(currency);
                    verifyTransactionInBlock(currency.getTransaction());
                    poll = createPoll(wallet, votingModel, name, plusFinishHeight, currency.getTransaction(), 1, maxRangeValue);
                    break;
            }
            verifyCreatingTransaction(poll);
            verifyTransactionInBlock(poll.getTransaction());
            currentHeight = getBlock().getHeight();
            pollId = poll.getTransaction();

            assertEquals(name, getPoll(pollId).getName(), "Names of poll are different");
            assertEquals(false, getPoll(pollId).getFinished(), "status of poll is different");

            vote = castVote(wallet, pollId, voteNumber);
            verifyCreatingTransaction(vote);
            verifyTransactionInBlock(vote.getTransaction());

            if (votingModel == POLL_BY_ACCOUNT_BALANCE) {
                weight = getBalance(wallet).getBalanceATM();
            }

            result = weight * voteNumber;
            log.info("result = " + result);
            log.info("weight = " + weight);
            log.info("voteNumber = " + voteNumber);
            waitForHeight(currentHeight + plusFinishHeight + 2);

            //getResults().get(INDEX) INDEX: YES = 0, No = 1, MAYBE = 2
            assertEquals(true, getPoll(pollId).getFinished(), "status of finished poll is different");
            assertEquals(Long.toString(weight), getPollResult(pollId).getResults().get(0).getWeight(), "amount of weights are different");
            assertEquals(Long.toString(result), getPollResult(pollId).getResults().get(0).getResult(), "amount of results are different");
            assertEquals(vote.getTransaction(), getPollVotes(pollId).getVotes().get(0).getTransaction(), "verification is failed on transaction Id");
            assertEquals(String.valueOf(voteNumber), getPollVotes(pollId).getVotes().get(0).getVotes().get(0), "votes are different");
            assertEquals("", getPollVotes(pollId).getVotes().get(0).getVotes().get(1), "votes are different");
            assertEquals("", getPollVotes(pollId).getVotes().get(0).getVotes().get(2), "votes are different");
            assertEquals(wallet.getUser(), getPollVotes(pollId).getVotes().get(0).getVoterRS(), "account ID's of voters are different");
        }


}

