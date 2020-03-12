package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.AccountDTO;
import com.apollocurrency.aplwallet.api.dto.Status2FA;
import com.apollocurrency.aplwallet.api.response.Account2FAResponse;
import com.apollocurrency.aplwallet.api.response.VaultWalletResponse;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.helper.WalletFactory;
import com.apollocurrrency.aplwallet.inttest.helper.providers.WalletProvider;
import com.apollocurrrency.aplwallet.inttest.model.TestBase;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
import java.util.Objects;

;import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Secret File")
@Epic(value = "Secret File")
public class Test2FA extends TestBase {

    @DisplayName("Delete Secret Key")
    @Test
    @Description("Delete Secret Key")
    public void deleteKey() {
        Wallet wallet = WalletFactory.getNewVaultWallet();
        Account2FAResponse deletedAccount = STEPS.ACCOUNT_STEPS.deleteSecretFile(wallet);
        assertEquals(Status2FA.OK, deletedAccount.getStatus());
    }


    @DisplayName("Export Secret Key")
    @Test
    public void exportKey(){
        Account2FAResponse accountDTO = STEPS.ACCOUNT_STEPS.generateNewAccount();
        Wallet wallet = new Wallet(accountDTO.getAccountRS(), accountDTO.getPassphrase());
        VaultWalletResponse secretFile = STEPS.ACCOUNT_STEPS.exportSecretFile(wallet);
        Assertions.assertTrue(secretFile.getFileName().contains(accountDTO.getAccountRS()));
        Assertions.assertNotNull(secretFile.getFileName());
    }

    @DisplayName("Import Secret Key")
    @Test
    public void importKey() throws IOException {
        Wallet wallet = TestConfiguration.getTestConfiguration().getVaultWallet();
        STEPS.ACCOUNT_STEPS.deleteSecretFile(wallet);
        ClassLoader classLoader = getClass().getClassLoader();
        String secretFilePath = Objects.requireNonNull(classLoader.getResource("APL-MK35-9X23-YQ5E-8QBKH")).getPath();
        boolean isKeyImpoted = STEPS.ACCOUNT_STEPS.importSecretFile(secretFilePath, "1");
        Assertions.assertTrue(isKeyImpoted,"Secret key isn't imported");
    }


    @DisplayName("Enable 2FA")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void enable2FATest(Wallet wallet) throws IOException {
        AccountDTO accountDTO = STEPS.ACCOUNT_STEPS.enable2FA(wallet);
        Assertions.assertNotNull(accountDTO.getSecret());
    }


}
