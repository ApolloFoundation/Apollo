package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.helper.providers.WalletProvider;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseNew;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DisplayName("Currencies")
@Epic(value = "Currencies")
public class TestCurrencies extends TestBaseNew {
    private final ArrayList<Wallet> wallets = new ArrayList<>();


    @BeforeEach
    @Override
    public void setUp(TestInfo testInfo) {
        super.setUp(testInfo);
        wallets.add(TestConfiguration.getTestConfiguration().getStandartWallet());
        wallets.add(TestConfiguration.getTestConfiguration().getVaultWallet());
    }

    @DisplayName("Issue Currencies")
    @ParameterizedTest(name = "{displayName} Currency type: {0} Wallet type: {1}")
    @MethodSource("currencyAll")
    public void issueCurrencys(int type,Wallet wallet) {
        log.info("Issue Currencies type: {}", type);
        int supply = RandomUtils.nextInt(0, 1000);
            CreateTransactionResponse currency = issueCurrency(wallet, type,
                    RandomStringUtils.randomAlphabetic(5),
                    RandomStringUtils.randomAlphabetic(5),
                    RandomStringUtils.randomAlphabetic(5).toUpperCase(),
                    supply,
                    supply,
                    RandomUtils.nextInt(0, 8));
            verifyCreatingTransaction(currency);

    }


    @DisplayName("Delete currency")
    @ParameterizedTest(name = "{displayName} Currency type: {0} Wallet type: {1}")
    @MethodSource("currencyAll")
    public void deleteCurrency(int type, Wallet wallet) {
        int supply = RandomUtils.nextInt(1, 1000);
            CreateTransactionResponse currency = issueCurrency(wallet, type,
                    RandomStringUtils.randomAlphabetic(5),
                    RandomStringUtils.randomAlphabetic(5),
                    RandomStringUtils.randomAlphabetic(5).toUpperCase(),
                    supply,
                    supply,
                    RandomUtils.nextInt(0, 8));
            verifyCreatingTransaction(currency);

            verifyTransactionInBlock(
                currency.getTransaction()
            );
            verifyTransactionInBlock(
              deleteCurrency(wallet, currency.getTransaction()).getTransaction()
            );

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
        assertThat(getCurrencyAccounts(currency.getTransaction()).getAccountCurrencies().size(), greaterThan(0));
        assertThat(getAllCurrencies().getCurrencies().size(), greaterThan(0));
    }

    @DisplayName("Transfer currency")
    @ParameterizedTest(name = "{displayName} Currency type: {0} Wallet type: {1}")
    @MethodSource("currencyExchangeable")
    public void transferCurrencyTest(int type,Wallet wallet) {
        int supply = RandomUtils.nextInt(0, 1000);
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


    @DisplayName("Mint Currencys")
    @ParameterizedTest(name = "{displayName} Currency type: {0} Wallet type: {1}")
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
    @ParameterizedTest(name = "{displayName} Currency type: {0} Wallet type: {1}")
    @MethodSource("currencyClaimableAndReservable")
    public void currencyReserveClaimTest(int type,Wallet wallet) {
        ArrayList<Wallet> wallets = new ArrayList<>();
        wallets.add(TestConfiguration.getTestConfiguration().getStandartWallet());
        wallets.add(TestConfiguration.getTestConfiguration().getVaultWallet());
        int supply = RandomUtils.nextInt(10, 1000);

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


    @DisplayName("Currency Reserve Increase ")
    @ParameterizedTest(name = "{displayName} Currency type: {0} Wallet type: {1}")
    @MethodSource("currencyClaimableAndReservable")
    public void currencyReserveIncreaseTest(int type, Wallet wallet) {
        int supply = RandomUtils.nextInt(1, 1000);
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

    @DisplayName("Exchange Offer")
    @ParameterizedTest(name = "{displayName} Currency type: {0} Wallet type: {1}")
    @MethodSource("currencyExchangeable")
    public void publishExchangeOfferTest(int type, Wallet wallet) {
        int supply = RandomUtils.nextInt(1, 1000);
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



    private static Stream<Arguments> currencyExchangeable() {
        List<Integer> types = Arrays.asList(1, 3, 17, 19, 33, 35, 51);
        return generateArgs(types);
    }

    private static Stream<Arguments> currencyClaimableAndReservable() {
        List<Integer> types = Arrays.asList(12, 13, 14, 15, 44, 45, 46, 47);
        return generateArgs(types);
    }
    private static Stream<Arguments> currencyAll() {
        List<Integer> types = Arrays.asList(1, 3, 5, 7, 12, 13, 14, 15, 17, 19, 21, 23, 33, 35, 37, 39, 44, 45, 46, 47, 51, 53, 55);
        return generateArgs(types);
    }



    private static Stream<Arguments> generateArgs(List<Integer> typesOfCurr){
        List<Wallet> wallets = Arrays.asList(
            TestConfiguration.getTestConfiguration().getStandartWallet(),
            TestConfiguration.getTestConfiguration().getVaultWallet());
        List<Arguments> arguments = new ArrayList<>();
        typesOfCurr.forEach(type-> wallets.forEach(wallet ->arguments.add(Arguments.of(type, wallet))));
        return arguments.stream();
    }

}
