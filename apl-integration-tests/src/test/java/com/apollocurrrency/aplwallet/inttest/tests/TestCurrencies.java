package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.helper.WalletProvider;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseOld;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Epic;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;

@DisplayName("Currencies")
@Epic(value = "Currencies")
public class TestCurrencies extends TestBaseOld {


    @DisplayName("Issue Currencys")
    @ParameterizedTest(name = "{displayName} Currency type: {0}")
    @ValueSource(ints = { 1,3,5,7,12,13,14,15,17,19,21,23,33,35,37,39,44,45,46,47,51,53,55 })
    public void issueCurrencys(int type){

        ArrayList<Wallet> wallets = new ArrayList<>();
        wallets.add(TestConfiguration.getTestConfiguration().getStandartWallet());
        wallets.add(TestConfiguration.getTestConfiguration().getVaultWallet());
        int supply  = RandomUtils.nextInt(0,1000);
        for (Wallet wallet: wallets) {
            CreateTransactionResponse currency = issueCurrency(wallet,type,
                    RandomStringUtils.randomAlphabetic(5),
                    RandomStringUtils.randomAlphabetic(5),
                    RandomStringUtils.randomAlphabetic(5).toUpperCase(),
                    supply,
                    supply,
                    RandomUtils.nextInt(0,8));
            verifyCreatingTransaction(currency);
        }
    }

}
