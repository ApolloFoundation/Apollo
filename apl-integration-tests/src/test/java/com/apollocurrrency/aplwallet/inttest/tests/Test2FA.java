package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.Account2FA;
import com.apollocurrency.aplwallet.api.dto.AccountDTO;
import com.apollocurrency.aplwallet.api.dto.Status2FA;
import com.apollocurrrency.aplwallet.inttest.helper.WalletProvider;
import com.apollocurrrency.aplwallet.inttest.model.TestBase;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class Test2FA extends TestBase {
    @DisplayName("Delete Secret Key")
    @Test
    public void  deleteKey() throws IOException { ;
        AccountDTO accountDTO = generateNewAccount();
        Wallet wallet = new Wallet(accountDTO.account,accountDTO.passphrase, null,"0");
        Account2FA deletedAccount = deleteKey(wallet);
        assertEquals(Status2FA.OK,deletedAccount.getStatus());
    }


    @DisplayName("Export Secret Key")
    @Test
    public void  exportKey() throws IOException {
        AccountDTO accountDTO = generateNewAccount();
        Wallet wallet = new Wallet(accountDTO.account,accountDTO.passphrase, null,"0");
        Account2FA exportKey = exportKey(wallet);
        assertEquals(accountDTO.accountRS,exportKey.accountRS);
        assertNotNull(exportKey.secretBytes);
    }

    @DisplayName("Import Secret Key")
    @Test
    public void  importKey() throws IOException {
        AccountDTO accountDTO = generateNewAccount();
        Wallet wallet = new Wallet(accountDTO.account,accountDTO.passphrase, null,"0");
        Account2FA exportKey = exportKey(wallet);
        wallet.setSecretKey(exportKey.secretBytes);
        deleteKey(wallet);
        Account2FA importKey = importKey(wallet);
        assertEquals(Status2FA.OK,importKey.getStatus());
    }

    @DisplayName("Enable 2FA")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void  enable2FATest(Wallet wallet) throws IOException { ;
        AccountDTO accountDTO = enable2FA(wallet);
        assertNotNull(accountDTO.secret);
    }
}
