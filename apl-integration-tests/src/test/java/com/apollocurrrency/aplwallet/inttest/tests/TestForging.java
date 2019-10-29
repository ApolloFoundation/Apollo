package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.Account;
import com.apollocurrency.aplwallet.api.dto.AccountDTO;
import com.apollocurrency.aplwallet.api.dto.ForgingDetails;
import com.apollocurrency.aplwallet.api.response.ForgingResponse;
import com.apollocurrency.aplwallet.api.response.GetAccountResponse;
import com.apollocurrrency.aplwallet.inttest.helper.WalletProvider;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseOld;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class TestForging extends TestBaseOld {


    @DisplayName("Start Get Stop Forging")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void  getAccountPropertyTest(Wallet wallet) throws IOException {
        GetAccountResponse accountDTO = getAccount(wallet.getUser());
        if (accountDTO.getEffectiveBalanceAPL() > 100000000000L) {
            ForgingDetails forgingDetails = startForging(wallet);
            assertTrue(forgingDetails.getHitTime() > 0);
            ForgingResponse getForgingResponse = getForging();
            assertNotNull(getForgingResponse.getGenerators().size() > 0);
            forgingDetails = stopForging(wallet);
            assertTrue(forgingDetails.getFoundAndStopped());
        }else{
            //TODO: Implement
        }
    }
}
