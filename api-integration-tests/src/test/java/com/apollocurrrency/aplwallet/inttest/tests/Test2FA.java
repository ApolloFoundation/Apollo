package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.Account2FA;
import com.apollocurrency.aplwallet.api.dto.AccountDTO;
import com.apollocurrency.aplwallet.api.dto.Status2FA;
import com.apollocurrrency.aplwallet.inttest.model.TestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class Test2FA extends TestBase {
    @DisplayName("Delete Secret Key")
    @Test
    public void  deleteKey() throws IOException { ;
        AccountDTO accountDTO = generateNewAccount();
        Account2FA deletedAccount = deleteKey(accountDTO.accountRS,accountDTO.passphrase);
        assertEquals(Status2FA.OK,deletedAccount.getStatus());
    }

    @DisplayName("Enable 2FA")
    @Test
    public void  enable2FA() throws IOException { ;
        AccountDTO accountDTO = generateNewAccount();
        accountDTO = enable2FA(accountDTO.accountRS,accountDTO.passphrase);
        assertNotNull(accountDTO.secret);
    }
}
