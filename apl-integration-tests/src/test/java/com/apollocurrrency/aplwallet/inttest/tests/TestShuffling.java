package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.account.AccountCurrencyDTO;
import com.apollocurrency.aplwallet.api.response.Account2FAResponse;
import com.apollocurrency.aplwallet.api.response.AccountAssetsResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrencyResponse;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrency.aplwallet.api.response.ShufflingDTO;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseOld;
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
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.ParameterizedTest;
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
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

@DisplayName("Shuffling")
@Epic(value = "Shuffling")
@Execution(SAME_THREAD)
public class TestShuffling extends TestBaseOld {
    public static final Logger log = LoggerFactory.getLogger(TestShuffling.class);
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
    private final ArrayList<Wallet> wallets = new ArrayList<>();
    RetryPolicy retry = new RetryPolicy()
        .retryWhen(false)
        .withMaxRetries(20)
        .withDelay(10, TimeUnit.SECONDS);
    private List<Wallet> recipients;
    private Wallet randomStandart;
    private Wallet randomVault;
    private CreateTransactionResponse shuffling = null;

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
    @ParameterizedTest(name = "{displayName} Currency type: {0}")
    @ValueSource(ints = {0, 1, 2})
    public void shufflingCreateTest(int type) {
        ShufflingDTO shufflingDTO;
        int registrationPeriod = RandomUtils.nextInt(500, 10080);
        randomStandart = getRandomStandartWallet();
        randomVault = getRandomVaultWallet();

        for (Wallet wallet : wallets) {
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
            ;

            waitForChangeShufflingStage(shuffling.getTransaction(), STAGE_REGISTRATION);

            shufflingRegister(randomStandart, shuffling.getFullHash());
            shufflingRegister(randomVault, shuffling.getFullHash());

            waitForChangeShufflingStage(shuffling.getTransaction(), STAGE_PROCESSING);

            recipients = Arrays.asList(
                getRandomRecipientWallet(),
                getRandomRecipientWallet(),
                getRandomRecipientWallet());

            verifyCreatingTransaction(
                shufflingProcess(wallet, shuffling.getTransaction(), recipients.get(2).getPass()));
            waitForChangeShufflingAssign(shuffling.getTransaction(), randomStandart.getUser());

            verifyCreatingTransaction(
                shufflingProcess(randomStandart, shuffling.getTransaction(), recipients.get(1).getPass()));
            waitForChangeShufflingAssign(shuffling.getTransaction(), randomVault.getUser());

            verifyCreatingTransaction(
                shufflingProcess(randomVault, shuffling.getTransaction(), recipients.get(0).getPass()));

            waitForChangeShufflingStage(shuffling.getTransaction(), STAGE_VERIFICATION);


            shufflingDTO = getShuffling(shuffling.getTransaction());
            shufflingVerify(wallet, shufflingDTO.getShuffling(), shufflingDTO.getShufflingStateHash());
            shufflingVerify(randomStandart, shufflingDTO.getShuffling(), shufflingDTO.getShufflingStateHash());

            waitForChangeShufflingStage(shuffling.getTransaction(), STAGE_DONE);
            assertShufflingDone(type, recipients);
        }
    }

    @DisplayName("Automation shuffling")
    @ParameterizedTest(name = "{displayName} Currency type: {0}")
    @ValueSource(ints = {0, 1, 2})
    public void shufflingCreateAutomationTest(int type) {
        int registrationPeriod = RandomUtils.nextInt(500, 10080);
        randomStandart = getRandomStandartWallet();
        randomVault = getRandomVaultWallet();

        for (Wallet wallet : wallets) {
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

            waitForChangeShufflingStage(shuffling.getTransaction(), STAGE_DONE);
            assertShufflingDone(type, recipients);


        }
    }


    @DisplayName("Blame shuffling")
    @ParameterizedTest(name = "{displayName} Currency type: {0}")
    @ValueSource(ints = {0, 1, 2})
    public void shufflingCancelTest(int type) {
        ShufflingDTO shufflingDTO;
        int registrationPeriod = RandomUtils.nextInt(500, 10080);

        randomStandart = getRandomStandartWallet();
        randomVault = getRandomVaultWallet();

        for (Wallet wallet : wallets) {
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
            ;

            waitForChangeShufflingStage(shuffling.getTransaction(), STAGE_REGISTRATION);

            shufflingRegister(randomStandart, shuffling.getFullHash());
            shufflingRegister(randomVault, shuffling.getFullHash());

            waitForChangeShufflingStage(shuffling.getTransaction(), STAGE_PROCESSING);

            recipients = Arrays.asList(
                getRandomRecipientWallet(),
                getRandomRecipientWallet(),
                getRandomRecipientWallet());


            verifyCreatingTransaction(
                shufflingProcess(wallet, shuffling.getTransaction(), recipients.get(2).getPass())
            );

            waitForChangeShufflingAssign(shuffling.getTransaction(), randomStandart.getUser());

            verifyCreatingTransaction(
                shufflingProcess(randomStandart, shuffling.getTransaction(), recipients.get(1).getPass())
            );

            waitForChangeShufflingAssign(shuffling.getTransaction(), randomVault.getUser());

            verifyTransactionInBlock(
                shufflingProcess(randomVault, shuffling.getTransaction(), recipients.get(0).getPass()).getTransaction()
            );

            waitForChangeShufflingStage(shuffling.getTransaction(), STAGE_VERIFICATION);

            shufflingDTO = getShuffling(shuffling.getTransaction());
            shufflingCancel(randomStandart, shufflingDTO.getShuffling(), null, shufflingDTO.getShufflingStateHash());

            shufflingDTO = getShuffling(shuffling.getTransaction());
            shufflingCancel(wallet, shufflingDTO.getShuffling(), null, shufflingDTO.getShufflingStateHash());

            waitForChangeShufflingStage(shuffling.getTransaction(), STAGE_BLAME);
            //TODO: Need implement assert STAGE_CANCELLED after 100 blocks

        }
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
        boolean isStage = Failsafe.with(retry).get(() -> getShuffling(shuffling).getStage() == stage);
        assertTrue(isStage, String.format("Stage isn't %s - > : %s", stage, getShuffling(shuffling).getStage()));
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
        sendMoney(randomVault, TestConfiguration.getTestConfiguration().getGenesisWallet().getUser(), (int) ((getBalance(randomVault).getUnconfirmedBalanceATM() - 1000000000L) / 100000000));
        if (getShuffling(shuffling.getTransaction()).getStage() == STAGE_DONE) {
            for (Wallet wallet : recipients) {
                sendMoney(wallet, TestConfiguration.getTestConfiguration().getGenesisWallet().getUser(), (int) ((getBalance(wallet).getUnconfirmedBalanceATM() - 1000000000L) / 100000000));
            }
        }
    }
}
