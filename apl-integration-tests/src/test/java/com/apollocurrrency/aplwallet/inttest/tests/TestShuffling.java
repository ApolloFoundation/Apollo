package com.apollocurrrency.aplwallet.inttest.tests;



import com.apollocurrency.aplwallet.api.dto.ShufflingParticipant;
import com.apollocurrency.aplwallet.api.dto.account.AccountCurrencyDTO;
import com.apollocurrency.aplwallet.api.response.Account2FAResponse;
import com.apollocurrency.aplwallet.api.response.AccountAssetsResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrencyResponse;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrency.aplwallet.api.response.ShufflingDTO;
import com.apollocurrency.aplwallet.api.response.ShufflingParticipantsResponse;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.helper.providers.ShufflingArgumentProvider;
import com.apollocurrrency.aplwallet.inttest.helper.providers.WalletProvider;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseNew;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DisplayName("Shuffling")
@Epic(value = "Shuffling")
public class TestShuffling extends TestBaseNew {
    public static final Logger log = LoggerFactory.getLogger(TestShuffling.class);

    RetryPolicy retry = new RetryPolicy()
            .retryWhen(false)
            .withMaxRetries(30)
            .withDelay(4, TimeUnit.SECONDS);

    private final int NON_SHUFFLEABLE = 32;

    private final int STAGE_REGISTRATION = 0;
    private final int STAGE_PROCESSING = 1;
    private final int STAGE_VERIFICATION = 2;
    private final int STAGE_BLAME = 3;
    private final int STAGE_CANCELLED = 4;
    private final int STAGE_DONE = 5;

    private final int PARTICIPANT_COUNT = 3;
    private final int APL_AMOUNT = 1000;
    private final int ASSET_AMOUNT = 1;
    private final int CURRENCY_AMOUNT = 1;

    private final int SHUFFLING_TYPE_APL = 0;
    private final int SHUFFLING_TYPE_ASSET = 1;
    private final int SHUFFLING_TYPE_CURRENCY = 2;


    private List<Wallet> recipients;
    private Wallet randomStandart;
    private Wallet randomVault;
    private CreateTransactionResponse shuffling = null;


    private final ArrayList<Wallet> wallets = new ArrayList<>();

    @BeforeEach
    @Override
    public void setUp(TestInfo testInfo) {
        super.setUp(testInfo);
        wallets.add(TestConfiguration.getTestConfiguration().getStandartWallet());

        wallets.add(TestConfiguration.getTestConfiguration().getVaultWallet());

        for (Wallet wallet : wallets) {
            setUpCurrency(wallet);
        }
    }

    @DisplayName("Manual shuffling")
    @ParameterizedTest(name = "{displayName} Shuffling type: {0}  Wallet type: {1}")
    @ArgumentsSource(ShufflingArgumentProvider.class)
     void shufflingCreateTest(int type, Wallet wallet) {
        log.info("Shuffling Type: "+type);
        ShufflingDTO shufflingDTO;
        int registrationPeriod = RandomUtils.nextInt(500, 10080);
        randomStandart = getRandomStandartWallet();
        randomVault = getRandomVaultWallet();
            switch (type) {
                case SHUFFLING_TYPE_APL:
                    shuffling = shufflingCreate(wallet, registrationPeriod, PARTICIPANT_COUNT, APL_AMOUNT, null, type);
                    break;

                case SHUFFLING_TYPE_ASSET:
                    AccountAssetsResponse assets = getAccountAssets(wallet);
                    String assetID = assets.getAccountAssets().stream()
                            .filter(asset -> asset.getQuantityATU() > 10).findFirst().get().getAsset();
                    assertNotNull(assets.getAccountAssets());
                    verifyTransactionInBlock(transferAsset(wallet, assetID, 3, randomStandart.getUser()).getTransaction());
                    verifyTransactionInBlock(transferAsset(wallet, assetID, 3, randomVault.getUser()).getTransaction());
                    shuffling = shufflingCreate(wallet, registrationPeriod, PARTICIPANT_COUNT, ASSET_AMOUNT, assetID, type);
                    break;

                case SHUFFLING_TYPE_CURRENCY:
                    AccountCurrencyResponse currencies = getAccountCurrencies(wallet);
                    assertNotNull(currencies.getAccountCurrencies());
                    AccountCurrencyDTO currencyDTO = currencies.getAccountCurrencies().stream()
                            .filter(currencie -> (currencie.getType() & NON_SHUFFLEABLE) != NON_SHUFFLEABLE)
                            .findFirst().get();
                    verifyTransactionInBlock(transferCurrency(randomStandart.getUser(), currencyDTO.getCurrency(), wallet, 3).getTransaction());
                    verifyTransactionInBlock(transferCurrency(randomVault.getUser(), currencyDTO.getCurrency(), wallet, 3).getTransaction());
                    shuffling = shufflingCreate(wallet, registrationPeriod, PARTICIPANT_COUNT, CURRENCY_AMOUNT, currencyDTO.getCurrency(), type);
                    ;
                    break;
            }

            log.info("Shuffling created " + shuffling.getTransaction());
            verifyCreatingTransaction(shuffling);
            verifyTransactionInBlock(shuffling.getTransaction());


            waitForChangeShufflingStage(shuffling.getTransaction(), STAGE_REGISTRATION);

            shufflingRegister(randomStandart, shuffling.getFullHash());
            shufflingRegister(randomVault, shuffling.getFullHash());

            waitForChangeShufflingStage(shuffling.getTransaction(), STAGE_PROCESSING);

            recipients = Arrays.asList(
                    getRandomRecipientWallet(),
                    getRandomRecipientWallet(),
                    getRandomRecipientWallet());
            waitForHeight(getBlock().getHeight()+1);

            int iteration = 0;
            while (iteration != PARTICIPANT_COUNT && getShuffling(shuffling.getTransaction()).getStage() == STAGE_PROCESSING) {
                shufflingDTO = getShuffling(shuffling.getTransaction());
                if (shufflingDTO.getAssigneeRS().equals(wallet.getUser())){
                    verifyTransactionInBlock(shufflingProcess(wallet, shufflingDTO.getShuffling(), recipients.get(2).getPass()).getTransaction());
                    log.info(String.format("Wallet: %s REGISTERED", wallet.getUser()));
                    iteration++;
                }else if (shufflingDTO.getAssigneeRS().equals(randomStandart.getUser())){
                    verifyTransactionInBlock(shufflingProcess(randomStandart, shufflingDTO.getShuffling(), recipients.get(1).getPass()).getTransaction());
                    log.info(String.format("Random Standart: %s REGISTERED", randomStandart.getUser()));
                    iteration++;
                }else {
                    verifyTransactionInBlock(shufflingProcess(randomVault, shufflingDTO.getShuffling(), recipients.get(0).getPass()).getTransaction());
                    log.info(String.format("Random Vault: %s REGISTERED", randomVault.getUser()));
                    iteration++;
                }
            }

            waitForChangeShufflingStage(shuffling.getTransaction(), STAGE_VERIFICATION);
            shufflingDTO = getShuffling(shuffling.getTransaction());

            ShufflingParticipantsResponse shufflingParticipantsResponse = getShufflingParticipants(shuffling.getTransaction());
            for (ShufflingParticipant participant: shufflingParticipantsResponse.getParticipants()) {
                if (participant.getState() == STAGE_PROCESSING && getShuffling(shuffling.getTransaction()).getStage() == STAGE_VERIFICATION){
                   if (participant.getAccountRS().equals(wallet.getUser())) {
                       verifyTransactionInBlock(shufflingVerify(wallet, shufflingDTO.getShuffling(), shufflingDTO.getShufflingStateHash()).getTransaction());
                   }else if(participant.getAccountRS().equals(randomStandart.getUser())){
                       verifyTransactionInBlock(shufflingVerify(randomStandart, shufflingDTO.getShuffling(), shufflingDTO.getShufflingStateHash()).getTransaction());
                   }else {
                       verifyTransactionInBlock(shufflingVerify(randomVault, shufflingDTO.getShuffling(), shufflingDTO.getShufflingStateHash()).getTransaction());
                   }
                }
            }

            waitForShufflingDeleted(shuffling.getTransaction());
            assertShufflingDone(type, recipients);
    }

    @DisplayName("Automation shuffling")
    @ParameterizedTest(name = "{displayName} Shuffling type: {0}  Wallet type: {1}")
    @ArgumentsSource(ShufflingArgumentProvider.class)
    public void shufflingCreateAutomationTest(int type,Wallet wallet) {
        int registrationPeriod = RandomUtils.nextInt(500, 10080);
        randomStandart = getRandomStandartWallet();
        randomVault = getRandomVaultWallet();


            switch (type) {
                case SHUFFLING_TYPE_APL:
                    shuffling = shufflingCreate(wallet, registrationPeriod, PARTICIPANT_COUNT, APL_AMOUNT, null, type);
                    break;

                case SHUFFLING_TYPE_ASSET:
                    AccountAssetsResponse assets = getAccountAssets(wallet);
                    String assetID = assets.getAccountAssets().stream()
                            .filter(asset -> asset.getQuantityATU() > 10).findFirst().get().getAsset();
                    assertNotNull(assets.getAccountAssets());
                    verifyTransactionInBlock(transferAsset(wallet, assetID, 3, randomStandart.getUser()).getTransaction());
                    verifyTransactionInBlock(transferAsset(wallet, assetID, 3, randomVault.getUser()).getTransaction());
                    shuffling = shufflingCreate(wallet, registrationPeriod, PARTICIPANT_COUNT, ASSET_AMOUNT, assetID, type);
                    break;

                case SHUFFLING_TYPE_CURRENCY:
                    AccountCurrencyResponse currencies = getAccountCurrencies(wallet);
                    assertNotNull(currencies.getAccountCurrencies());
                    AccountCurrencyDTO currencyDTO = currencies.getAccountCurrencies().stream()
                            .filter(currencie -> (currencie.getType() & NON_SHUFFLEABLE) != NON_SHUFFLEABLE)
                            .findFirst().get();
                    verifyTransactionInBlock(transferCurrency(randomStandart.getUser(), currencyDTO.getCurrency(), wallet, 3).getTransaction());
                    verifyTransactionInBlock(transferCurrency(randomVault.getUser(), currencyDTO.getCurrency(), wallet, 3).getTransaction());
                    shuffling = shufflingCreate(wallet, registrationPeriod, PARTICIPANT_COUNT, CURRENCY_AMOUNT, currencyDTO.getCurrency(), type);
                    break;
            }
            log.info("Shuffling created " + shuffling.getTransaction());
            verifyCreatingTransaction(shuffling);
            verifyTransactionInBlock(shuffling.getTransaction());
            recipients = Arrays.asList(
                    getRandomRecipientWallet(),
                    getRandomRecipientWallet(),
                    getRandomRecipientWallet());


            startShuffler(wallet, shuffling.getFullHash(), recipients.get(0).getPass());
            startShuffler(randomStandart, shuffling.getFullHash(), recipients.get(1).getPass());
            startShuffler(randomVault, shuffling.getFullHash(), recipients.get(2).getPass());

            log.info("Shuffling started " + shuffling.getTransaction());
            waitForShufflingDeleted(shuffling.getTransaction());
            assertShufflingDone(type, recipients);

    }


    @DisplayName("Blame shuffling")
    @ParameterizedTest(name = "{displayName} Shuffling type: {0}  Wallet type: {1}")
    @ArgumentsSource(ShufflingArgumentProvider.class)
    void shufflingCancelTest(int type,Wallet wallet) {
        ShufflingDTO shufflingDTO;
        int registrationPeriod = RandomUtils.nextInt(500, 10080);

        randomStandart = getRandomStandartWallet();
        randomVault = getRandomVaultWallet();
            switch (type) {
                case SHUFFLING_TYPE_APL:
                    shuffling = shufflingCreate(wallet, registrationPeriod, PARTICIPANT_COUNT, APL_AMOUNT, null, type);
                    break;

                case SHUFFLING_TYPE_ASSET:
                    AccountAssetsResponse assets = getAccountAssets(wallet);
                    String assetID = assets.getAccountAssets().stream()
                            .filter(asset -> asset.getQuantityATU() > 10).findFirst().get().getAsset();
                    assertNotNull(assets.getAccountAssets());
                    verifyTransactionInBlock(transferAsset(wallet, assetID, 3, randomStandart.getUser()).getTransaction());
                    verifyTransactionInBlock(transferAsset(wallet, assetID, 3, randomVault.getUser()).getTransaction());
                    shuffling = shufflingCreate(wallet, registrationPeriod, PARTICIPANT_COUNT, ASSET_AMOUNT, assetID, type);
                    break;

                case SHUFFLING_TYPE_CURRENCY:
                    AccountCurrencyResponse currencies = getAccountCurrencies(wallet);
                    assertNotNull(currencies.getAccountCurrencies());
                    AccountCurrencyDTO currencyDTO = currencies.getAccountCurrencies().stream()
                            .filter(currencie -> (currencie.getType() & NON_SHUFFLEABLE) != NON_SHUFFLEABLE)
                            .findFirst().get();
                    verifyTransactionInBlock(transferCurrency(randomStandart.getUser(), currencyDTO.getCurrency(), wallet, 3).getTransaction());
                    verifyTransactionInBlock(transferCurrency(randomVault.getUser(), currencyDTO.getCurrency(), wallet, 3).getTransaction());
                    shuffling = shufflingCreate(wallet, registrationPeriod, PARTICIPANT_COUNT, CURRENCY_AMOUNT, currencyDTO.getCurrency(), type);
                    ;
                    break;
            }

            log.info("Shuffling created " + shuffling.getTransaction());
            verifyCreatingTransaction(shuffling);
            verifyTransactionInBlock(shuffling.getTransaction());


            waitForChangeShufflingStage(shuffling.getTransaction(), STAGE_REGISTRATION);

            shufflingRegister(randomStandart, shuffling.getFullHash());
            shufflingRegister(randomVault, shuffling.getFullHash());

            waitForChangeShufflingStage(shuffling.getTransaction(), STAGE_PROCESSING);

            recipients = Arrays.asList(
                    getRandomRecipientWallet(),
                    getRandomRecipientWallet(),
                    getRandomRecipientWallet());


            int iteration = 0;
            while (iteration != PARTICIPANT_COUNT) {
                shufflingDTO = getShuffling(shuffling.getTransaction());
                if (shufflingDTO.getAssigneeRS().equals(wallet.getUser())){
                    verifyTransactionInBlock(shufflingProcess(wallet, shufflingDTO.getShuffling(), recipients.get(2).getPass()).getTransaction());
                    log.info(String.format("Wallet: %s REGISTERED", wallet.getUser()));
                    iteration++;
                }else if (shufflingDTO.getAssigneeRS().equals(randomStandart.getUser())){
                    verifyTransactionInBlock(shufflingProcess(randomStandart, shufflingDTO.getShuffling(), recipients.get(1).getPass()).getTransaction());
                    log.info(String.format("Random Standart: %s REGISTERED", randomStandart.getUser()));
                    iteration++;
                }else {
                    verifyTransactionInBlock(shufflingProcess(randomVault, shufflingDTO.getShuffling(), recipients.get(0).getPass()).getTransaction());
                    log.info(String.format("Random Vault: %s REGISTERED", randomVault.getUser()));
                    iteration++;
                }
            }

            waitForChangeShufflingStage(shuffling.getTransaction(), STAGE_VERIFICATION);

            shufflingDTO = getShuffling(shuffling.getTransaction());
            shufflingCancel(randomStandart, shufflingDTO.getShuffling(), null, shufflingDTO.getShufflingStateHash());

            shufflingDTO = getShuffling(shuffling.getTransaction());
            shufflingCancel(wallet, shufflingDTO.getShuffling(), null, shufflingDTO.getShufflingStateHash());

            waitForChangeShufflingStage(shuffling.getTransaction(), STAGE_BLAME);
            //TODO: Need implement assert STAGE_CANCELLED after 100 blocks

    }


    @Step
    private void assertShufflingDone(int type, List<Wallet> recipients) {
        switch (type) {

            case SHUFFLING_TYPE_APL:
                for (Wallet recipient : recipients) {
                    assertEquals(APL_AMOUNT, getBalance(recipient).getBalanceATM() / 100000000);
                }
                break;

            case SHUFFLING_TYPE_ASSET:
                for (Wallet recipient : recipients) {
                    assertEquals(1, getAccountAssets(recipient).getAccountAssets().size());
                }
                break;

            case SHUFFLING_TYPE_CURRENCY:
                for (Wallet recipient : recipients) {
                    assertEquals(1, getAccountCurrencies(recipient).getAccountCurrencies().size());
                }
                break;

        }
    }

    @Step
    private Wallet getRandomStandartWallet() {
        String randomPass = String.valueOf(RandomUtils.nextInt(1, 199));
        Wallet wallet = new Wallet(getAccountId(randomPass).getAccountRS(), randomPass);
        log.info(String.format("Standard Wallet: %s pass: %s", wallet.getUser(), wallet.getPass()));
        return wallet;
    }

    @Step
    private Wallet getRandomRecipientWallet() {
        String randomPass = RandomStringUtils.randomAlphabetic(10);
        log.info("Recipient SecretPhrase: " + randomPass);
        return new Wallet(getAccountId(randomPass).getAccountRS(), randomPass);
    }

    @Step
    private Wallet getRandomVaultWallet() {
        Account2FAResponse account = generateNewAccount();
        Wallet vaultWallet = new Wallet(account.getAccountRS(), account.getPassphrase(), true);
        log.info(String.format("Vault Wallet: %s pass: %s", vaultWallet.getUser(), vaultWallet.getPass()));

        verifyTransactionInBlock(
                sendMoney(TestConfiguration.getTestConfiguration().getGenesisWallet(), vaultWallet.getUser(), 10000).getTransaction()
        );
        verifyTransactionInBlock(
                sendMoney(vaultWallet, vaultWallet.getUser(), 10).getTransaction()
        );
        return vaultWallet;
    }

    @Step
    private void setUpCurrency(Wallet wallet) {
        issueCurrency(wallet, 1,
                RandomStringUtils.randomAlphabetic(5),
                RandomStringUtils.randomAlphabetic(5),
                RandomStringUtils.randomAlphabetic(5).toUpperCase(),
                150,
                150,
                0);
    }

    @Step
    private void waitForChangeShufflingStage(String shuffling, int stage) {
        log.info("Check Shuffling stage: " + stage);
        boolean isStage = Failsafe.with(retry).get(() -> getShuffling(shuffling).getStage() == stage);
        assertTrue(isStage, String.format("Stage isn't %s - > : %s", stage, getShuffling(shuffling).getStage()));
    }

    @Step
    private void waitForShufflingDeleted(String shuffling) {
       boolean isDeleted = Failsafe.with(retry).get(() -> getAllShufflings().getShufflings().stream().noneMatch(shuff -> shuff.getShuffling().equals(shuffling)));
       assertTrue(isDeleted, String.format("Isn't deleted %s - > : %s", isDeleted, getAllShufflings().getShufflings().stream().noneMatch(shuff -> shuff.getShuffling().equals(shuffling))));
    }

    @Step
    private void waitForChangeShufflingAssign(String shuffling, String account) {
        boolean isAssigned = Failsafe.with(retry).get(() -> getShuffling(shuffling).getAssigneeRS().equals(account));
        assertTrue(isAssigned, String.format("Account isn't assigned %s - > : %s", account, getShuffling(shuffling).getAssigneeRS()));
    }


    @AfterEach
    @Override
    public void tearDown() {
        super.tearDown();
        sendMoney(randomVault, TestConfiguration.getTestConfiguration().getGenesisWallet().getUser(), (int) ((getBalance(randomVault).getBalanceATM() - 1000000000L) / 100000000));
            for (Wallet wallet : recipients) {
                sendMoney(wallet, TestConfiguration.getTestConfiguration().getGenesisWallet().getUser(), (int) ((getBalance(wallet).getBalanceATM() - 1000000000L) / 100000000));
            }
        }

}
