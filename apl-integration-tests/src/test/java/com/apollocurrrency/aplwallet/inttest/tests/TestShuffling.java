package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.AccountCurrencyDTO;
import com.apollocurrency.aplwallet.api.response.AccountAssetsResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrencyResponse;
import com.apollocurrency.aplwallet.api.response.AssetsResponse;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrency.aplwallet.api.response.CurrenciesResponse;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseOld;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestShuffling extends TestBaseOld {
    private final ArrayList<Wallet> wallets = new ArrayList<>();
    @BeforeEach
    @Override
    public void setUP(TestInfo testInfo) {
                super.setUP(testInfo);
        wallets.add(TestConfiguration.getTestConfiguration().getStandartWallet());
        wallets.add(TestConfiguration.getTestConfiguration().getVaultWallet());
    }


    @DisplayName("Issue Currencys")
    @ParameterizedTest(name = "{displayName} Currency type: {0}")
    @ValueSource(ints = { 0,1,2})
    @Tag("NEGATIVE")
    public void shufflingCreateTest(int type){
        int  registrationPeriod  = RandomUtils.nextInt(100,10080);

        for (Wallet wallet: wallets) {
            switch (type){
                case 0:
                    shufflingCreate(wallet, registrationPeriod,3,1000,null,type);
                    break;
                case 1:
                     AccountAssetsResponse assets  =  getAccountAssets(wallet);
                     assertNotNull(assets.getAccountAssets());
                     shufflingCreate(wallet, registrationPeriod,3,1,assets.getAccountAssets().get(0).getAsset(),type);
                    break;
                case 2:
                    AccountCurrencyResponse currencies  = getAccountCurrencies(wallet);
                    assertNotNull(currencies.getAccountCurrencies());
                    AccountCurrencyDTO currencyDTO = currencies.getAccountCurrencies().stream()
                            .filter(currencie -> Integer.valueOf(currencie.getUnits()) > Integer.valueOf(currencie.getUnconfirmedUnits())).findFirst().get();
                    CreateTransactionResponse shuffling =  shufflingCreate(wallet, registrationPeriod,3,1,currencyDTO.getCurrency(),type);
                    verifyTransactionInBlock(shuffling.getTransaction());
                     break;
            }
        }
    }
}
