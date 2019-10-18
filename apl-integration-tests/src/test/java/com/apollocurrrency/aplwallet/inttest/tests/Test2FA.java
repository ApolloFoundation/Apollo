package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.*;
import com.apollocurrency.aplwallet.api.response.Account2FAResponse;
import com.apollocurrrency.aplwallet.inttest.helper.WalletProvider;
import com.apollocurrrency.aplwallet.inttest.model.TestBase;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseNew;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseOld;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class Test2FA extends TestBaseNew {
    /*
    @DisplayName("Delete Secret Key")
    @Test
    public void  deleteKey() {
        AccountDTO accountDTO = generateNewAccount();
        Wallet wallet = new Wallet(accountDTO.getAccount(),accountDTO.getPassphrase(), null,"0");
        Account2FAResponse deletedAccount = deleteSecretFile(wallet);
        assertEquals(Status2FA.OK,deletedAccount.getStatus());
    }

*/
    @DisplayName("Export Secret Key")
    @Test
    public void  exportKey(){
        AccountDTO accountDTO = generateNewAccount();
        Wallet wallet = new Wallet(accountDTO.getAccountRS(),accountDTO.getPassphrase(), null,"0");
        Account2FAResponse exportKey = exportSecretFile(wallet);
        assertEquals(accountDTO.getAccountRS(),exportKey.getAccountRS());
        assertNotNull(exportKey.getSecretBytes());
    }

    @DisplayName("Import Secret Key")
    @Test
    public void  importKey() throws IOException {
        AccountDTO accountDTO = generateNewAccount();
        Wallet wallet = new Wallet(accountDTO.getAccount(),accountDTO.getPassphrase(), null,"0");
        Account2FAResponse exportKey = exportSecretFile(wallet);
        wallet.setSecretKey(exportKey.getSecretBytes());
        deleteSecretFile(wallet);
        Account2FAResponse importKey = importSecretFile(wallet);
        assertEquals(Status2FA.OK,importKey.getStatus());
    }

    /*
    @DisplayName("Enable 2FA")
    @ParameterizedTest
    @ArgumentsSource(WalletProvider.class)
    public void  enable2FATest(Wallet wallet) throws IOException { ;
        AccountDTO accountDTO = enable2FA(wallet);
        assertNotNull(accountDTO.getSecret());
    }
     */

}
