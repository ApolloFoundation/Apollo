package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.helper.WalletProvider;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseOld;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

@DisplayName("Currencies")
@Epic(value = "Currencies")
@Execution(SAME_THREAD)
public class TestCurrencies extends TestBaseOld {
    private final ArrayList<Wallet> wallets = new ArrayList<>();

    @BeforeEach
    @Override
    public void setUp(TestInfo testInfo) {
        super.setUp(testInfo);
        wallets.add(TestConfiguration.getTestConfiguration().getStandartWallet());
        wallets.add(TestConfiguration.getTestConfiguration().getVaultWallet());
    }

    @DisplayName("Issue Currencies")
    @ParameterizedTest(name = "{displayName} Currency type: {0}")
    @ValueSource(ints = {1, 3, 5, 7, 12, 13, 14, 15, 17, 19, 21, 23, 33, 35, 37, 39, 44, 45, 46, 47, 51, 53, 55})
    public void issueCurrencys(int type) {
        log.info("Issue Currencies type: {}", type);
        int supply = RandomUtils.nextInt(0, 1000);
        for (Wallet wallet : wallets) {
            CreateTransactionResponse currency = issueCurrency(wallet, type,
                RandomStringUtils.randomAlphabetic(5),
                RandomStringUtils.randomAlphabetic(5),
                RandomStringUtils.randomAlphabetic(5).toUpperCase(),
                supply,
                supply,
                RandomUtils.nextInt(0, 8));
            verifyCreatingTransaction(currency);
        }
    }


    @DisplayName("Delete currency")
    @ParameterizedTest(name = "{displayName} Currency type: {0}")
    @ValueSource(ints = {1, 3, 17, 19, 33, 35, 51})
    public void deleteCurrency(int type) {
        int supply = RandomUtils.nextInt(1, 1000);
        for (Wallet wallet : wallets) {
            CreateTransactionResponse currency = issueCurrency(wallet, type,
                RandomStringUtils.randomAlphabetic(5),
                RandomStringUtils.randomAlphabetic(5),
                RandomStringUtils.randomAlphabetic(5).toUpperCase(),
                supply,
                supply,
                RandomUtils.nextInt(0, 8));
            verifyCreatingTransaction(currency);
            verifyTransactionInBlock(currency.getTransaction());
            deleteCurrency(wallet, currency.getTransaction());
        }
    }

    @DisplayName("Get ( currency /  currency accounts / all) ")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void deleteCurrency(Wallet wallet) {
        int supply = RandomUtils.nextInt(0, 1000);
        CreateTransactionResponse currency = issueCurrency(wallet, 1,
            RandomStringUtils.randomAlphabetic(5),
            RandomStringUtils.randomAlphabetic(5),
            RandomStringUtils.randomAlphabetic(5).toUpperCase(),
            supply,
            supply,
            RandomUtils.nextInt(0, 8));
        verifyCreatingTransaction(currency);
        verifyTransactionInBlock(currency.getTransaction());
        assertEquals(wallet.getUser(), getCurrency(currency.getTransaction()).getAccountRS());
        assertEquals(1, getCurrency(currency.getTransaction()).getType());
        assertTrue(getCurrencyAccounts(currency.getTransaction()).getAccountCurrencies().size() > 0);
        assertTrue(getAllCurrencies().getCurrencies().size() > 0);
    }

    @DisplayName("Transfer currency")
    @ParameterizedTest(name = "{displayName} Currency type: {0}")
    @ValueSource(ints = {1, 3, 17, 19, 33, 35, 51})
    public void transferCurrencyTest(int type) {
        int supply = RandomUtils.nextInt(0, 1000);
        for (Wallet wallet : wallets) {
            CreateTransactionResponse currency = issueCurrency(wallet, type,
                RandomStringUtils.randomAlphabetic(5),
                RandomStringUtils.randomAlphabetic(5),
                RandomStringUtils.randomAlphabetic(5).toUpperCase(),
                supply,
                supply,
                RandomUtils.nextInt(0, 8));
            verifyCreatingTransaction(currency);
            verifyTransactionInBlock(currency.getTransaction());
            CreateTransactionResponse transaction = transferCurrency(TestConfiguration.getTestConfiguration().getGenesisWallet().getUser(), currency.getTransaction(), wallet, 1);
            verifyTransactionInBlock(transaction.getTransaction());
            assertTrue(getCurrencyAccounts(currency
                .getTransaction())
                .getAccountCurrencies().stream()
                .anyMatch(account -> account.getAccountRS().equals(TestConfiguration.getTestConfiguration().getGenesisWallet().getUser())));
        }
    }


    @DisplayName("Mint Currencys")
    @ParameterizedTest(name = "{displayName} Currency type: {0}")
    @ValueSource(ints = {17})
    @Disabled
    public void currencyMint(int type) {
        int supply = RandomUtils.nextInt(100000000, 1000000000);
        for (Wallet wallet : wallets) {
            CreateTransactionResponse currency = issueCurrency(wallet, type,
                RandomStringUtils.randomAlphabetic(5),
                RandomStringUtils.randomAlphabetic(5),
                RandomStringUtils.randomAlphabetic(5).toUpperCase(),
                supply,
                supply,
                RandomUtils.nextInt(0, 8));
            verifyCreatingTransaction(currency);
            verifyTransactionInBlock(currency.getTransaction());
            //TODO: Need implement Mint Worker

        }
    }

    @DisplayName("Reserve Claim Currencys")
    @ParameterizedTest(name = "{displayName} Currency type: {0}")
    @ValueSource(ints = {12, 13, 14, 15, 44, 45, 46, 47})
    public void currencyReserveClaimTest(int type) {
        ArrayList<Wallet> wallets = new ArrayList<>();
        wallets.add(TestConfiguration.getTestConfiguration().getStandartWallet());
        wallets.add(TestConfiguration.getTestConfiguration().getVaultWallet());
        int supply = RandomUtils.nextInt(10, 1000);
        for (Wallet wallet : wallets) {
            log.info("Issue Currencies type: {}", type);
            CreateTransactionResponse currency = issueCurrency(wallet, type,
                RandomStringUtils.randomAlphabetic(5),
                RandomStringUtils.randomAlphabetic(5),
                RandomStringUtils.randomAlphabetic(5).toUpperCase(),
                supply,
                supply,
                0);
            verifyCreatingTransaction(currency);
            verifyTransactionInBlock(currency.getTransaction());
            CreateTransactionResponse reserveTransaction = currencyReserveIncrease(currency.getTransaction(), wallet, supply + 10);
            verifyTransactionInBlock(reserveTransaction.getTransaction());
            log.info("Reserve Currencys: {}", reserveTransaction.getTransaction());
            waitForHeight(getCurrency(currency.getTransaction()).getIssuanceHeight());
            CreateTransactionResponse reserveClaimTransaction = currencyReserveClaim(currency.getTransaction(), wallet, 1);
            verifyCreatingTransaction(reserveClaimTransaction);
            //EXCHANGEABLE - 1
            if ((type & 1) == 1) {
                verifyTransactionInBlock(reserveClaimTransaction.getTransaction());
                exchange(currency, wallet);

            }
        }
    }


    @DisplayName("Currency Reserve Increase ")
    @ParameterizedTest(name = "{displayName} Currency type: {0}")
    @ValueSource(ints = {12, 13, 14, 15, 44, 45, 46, 47})
    public void currencyReserveIncreaseTest(int type) {
        int supply = RandomUtils.nextInt(1, 1000);
        for (Wallet wallet : wallets) {
            CreateTransactionResponse currency = issueCurrency(wallet, type,
                RandomStringUtils.randomAlphabetic(5),
                RandomStringUtils.randomAlphabetic(5),
                RandomStringUtils.randomAlphabetic(5).toUpperCase(),
                supply,
                supply,
                0);
            verifyCreatingTransaction(currency);
            verifyTransactionInBlock(currency.getTransaction());
            CreateTransactionResponse reserveTransaction = currencyReserveIncrease(currency.getTransaction(), wallet, supply + 1);
            verifyCreatingTransaction(reserveTransaction);
        }
    }

    @DisplayName("Exchange Offer")
    @ParameterizedTest(name = "{displayName} Currency type: {0}")
    @ValueSource(ints = {1, 3, 17, 19, 33, 35, 51})
    public void publishExchangeOfferTest(int type) {
        int supply = RandomUtils.nextInt(1, 1000);
        for (Wallet wallet : wallets) {
            CreateTransactionResponse currency = issueCurrency(wallet, type,
                RandomStringUtils.randomAlphabetic(5),
                RandomStringUtils.randomAlphabetic(5),
                RandomStringUtils.randomAlphabetic(5).toUpperCase(),
                supply,
                supply,
                RandomUtils.nextInt(0, 8));
            verifyCreatingTransaction(currency);
            exchange(currency, wallet);

        }
    }

    @Step
    //TODO: Need implement amount verification after exchange
    private void exchange(CreateTransactionResponse currency, Wallet wallet) {
        verifyTransactionInBlock(currency.getTransaction());
        CreateTransactionResponse offer = publishExchangeOffer(currency.getTransaction(), wallet, 1, 1, 1, 1);
        verifyTransactionInBlock(offer.getTransaction());
        Wallet gen_wallet = TestConfiguration.getTestConfiguration().getGenesisWallet();
        CreateTransactionResponse sellTransaction = currencySell(currency.getTransaction(), wallet, 1, 1);
        verifyCreatingTransaction(sellTransaction);
        CreateTransactionResponse buyTransaction = currencyBuy(currency.getTransaction(), gen_wallet, 1, 1);
        verifyCreatingTransaction(buyTransaction);
        CreateTransactionResponse scheduledbuyTransaction = scheduleCurrencyBuy(currency.getTransaction(), gen_wallet, 1, 1, wallet.getUser());
        verifyCreatingTransaction(scheduledbuyTransaction);
    }

}
