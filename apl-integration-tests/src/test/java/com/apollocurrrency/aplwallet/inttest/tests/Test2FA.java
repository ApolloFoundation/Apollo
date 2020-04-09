package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.Status2FA;
import com.apollocurrency.aplwallet.api.dto.account.AccountDTO;
import com.apollocurrency.aplwallet.api.response.Account2FAResponse;
import com.apollocurrency.aplwallet.api.response.VaultWalletResponse;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.helper.WalletProvider;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseNew;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
import java.util.Objects;

;

@DisplayName("Secret File")
@Epic(value = "Secret File")
public class Test2FA extends TestBaseNew {

    @DisplayName("Delete Secret Key")
    @Test
    @Description("Delete Secret Key")
    public void deleteKey() throws JsonProcessingException {

        Account2FAResponse accountDTO = generateNewAccount();
        Wallet wallet = new Wallet(accountDTO.getAccount(), accountDTO.getPassphrase(), null, true, null, null);
        Account2FAResponse deletedAccount = deleteSecretFile(wallet);
        Assertions.assertEquals(Status2FA.OK, deletedAccount.getStatus());

    }


    @DisplayName("Export Secret Key")
    @Test
    public void exportKey() throws JsonProcessingException {
        Account2FAResponse accountDTO = generateNewAccount();
        Wallet wallet = new Wallet(accountDTO.getAccountRS(), accountDTO.getPassphrase(), null, true, null, null);
        VaultWalletResponse secretFile = exportSecretFile(wallet);
        Assertions.assertTrue(secretFile.getFileName().contains(accountDTO.getAccountRS()));
        Assertions.assertNotNull(secretFile.getFileName());
    }

    @DisplayName("Import Secret Key")
    @Test
    public void importKey() throws IOException {
        Wallet wallet = TestConfiguration.getTestConfiguration().getVaultWallet();
        deleteSecretFile(wallet);
        ClassLoader classLoader = getClass().getClassLoader();
        String secretFilePath = Objects.requireNonNull(classLoader.getResource("APL-MK35-9X23-YQ5E-8QBKH")).getPath();
        boolean isKeyImpoted = importSecretFile(secretFilePath, "1");
        Assertions.assertTrue(isKeyImpoted);
    }


    @DisplayName("Enable 2FA")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void enable2FATest(Wallet wallet) throws IOException {
        ;
        AccountDTO accountDTO = enable2FA(wallet);
        Assertions.assertNotNull(accountDTO.getSecret());
    }


}
