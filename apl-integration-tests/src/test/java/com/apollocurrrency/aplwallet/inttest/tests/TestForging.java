package com.apollocurrrency.aplwallet.inttest.tests;


import com.apollocurrency.aplwallet.api.dto.ForgingDetails;
import com.apollocurrency.aplwallet.api.response.ForgingResponse;
import com.apollocurrency.aplwallet.api.response.GetAccountResponse;
import com.apollocurrrency.aplwallet.inttest.helper.providers.WalletProvider;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseNew;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Epic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Forging")
@Epic(value = "Forging")
public class TestForging extends TestBaseNew {


    @DisplayName("Start Get Stop Forging")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    public void getAccountPropertyTest(Wallet wallet) {
        GetAccountResponse accountDTO = getAccount(wallet.getUser());
        if (accountDTO.getEffectiveBalanceAPL() > 100000000000L) {
            ForgingDetails forgingDetails = startForging(wallet);
            assertTrue(forgingDetails.getHitTime() > 0);
            ForgingResponse getForgingResponse = getForging();
            assertNotNull(getForgingResponse.getGenerators().size() > 0);
            forgingDetails = stopForging(wallet);
            assertTrue(forgingDetails.getFoundAndStopped());
        }
    }
}
