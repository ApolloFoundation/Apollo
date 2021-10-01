/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.WalletDTO;
import com.apollocurrency.aplwallet.api.dto.account.CurrenciesWalletsDTO;
import com.apollocurrency.aplwallet.api.dto.account.CurrencyWalletsDTO;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.service.appdata.SecureStorageService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.service.ElGamalEncryptor;
import com.apollocurrency.aplwallet.vault.model.AplWalletKey;
import com.apollocurrency.aplwallet.vault.model.ApolloFbWallet;
import com.apollocurrency.aplwallet.vault.model.EthWalletKey;
import com.apollocurrency.aplwallet.vault.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.vault.service.KMSService;
import lombok.SneakyThrows;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableWeld
@ExtendWith(MockitoExtension.class)
class KeyStoreControllerTest extends AbstractEndpointTest {
    int maxKeystoreFileSize = 500;
    @Mock
    KMSService kmsService;
    @Mock
    SecureStorageService secureStorageService;
    ElGamalEncryptor encryptor = mock(ElGamalEncryptor.class);

    @WeldSetup
    WeldInitiator weldInitiator = WeldInitiator.from()
        .addBeans(MockBean.of(encryptor, ElGamalEncryptor.class)) // only for static CDI lookups
        .build();


    @Override
    @BeforeEach
    void setUp() {
        super.setUp();
        dispatcher.getRegistry().addSingletonResource( new KeyStoreController(kmsService, secureStorageService, maxKeystoreFileSize));
    }

    @AfterEach
    void tearDown() {
        HttpParameterParserUtil.resetCDIComponents(); // cleanup CDI lookups
    }

    @Test
    @SneakyThrows
    void getAccountInfo_noPassphrase() {
        MockHttpResponse response = sendPostRequest("/keyStore/accountInfo", "account=" + Long.toUnsignedString(ACCOUNT_ID_WITH_SECRET));

        assertEquals(200, response.getStatus(), "getAccountInfo error response should be always with code 200");
        Error error = mapper.readValue(response.getContentAsString(), Error.class);
        assertEquals(new Error("\"passphrase\" not specified", 3, 0, 0, 0), error);
        verify(encryptor, never()).elGamalDecrypt(anyString());
    }


    @Test
    @SneakyThrows
    void getAccountInfo_walletNotExist() {
        when(encryptor.elGamalDecrypt(PASSPHRASE)).thenReturn(PASSPHRASE);

        MockHttpResponse response = sendPostRequest("/keyStore/accountInfo", "account=" + Long.toUnsignedString(ACCOUNT_ID_WITH_SECRET)
                + "&passphrase=" + PASSPHRASE);

        assertEquals(200, response.getStatus(), "getAccountInfo error response should be always with code 200");
        Error error = mapper.readValue(response.getContentAsString(), Error.class);
        assertEquals(new Error("Key for this account is not exist.", 22, 1, 2014, 0), error);
    }

    @Test
    @SneakyThrows
    void getAccountInfo_noKeystore() {
        when(encryptor.elGamalDecrypt(PASSPHRASE)).thenReturn(PASSPHRASE);
        when(kmsService.isWalletExist(ACCOUNT_ID_WITH_SECRET)).thenReturn(true);

        MockHttpResponse response = sendPostRequest("/keyStore/accountInfo", "account=" + Long.toUnsignedString(ACCOUNT_ID_WITH_SECRET)
            + "&passphrase=" + PASSPHRASE);

        assertEquals(200, response.getStatus(), "getAccountInfo error response should be always with code 200");
        Error error = mapper.readValue(response.getContentAsString(), Error.class);
        assertEquals(new Error("KeyStore or passPhrase is not valid.", 22, 1, 2014, 0), error);
    }


    @Test
    @SneakyThrows
    void getAccountInfo_OK() {
        when(encryptor.elGamalDecrypt(PASSPHRASE)).thenReturn(PASSPHRASE);
        when(kmsService.isWalletExist(ACCOUNT_ID_WITH_SECRET)).thenReturn(true);
        ApolloFbWallet wallet = new ApolloFbWallet();
        wallet.addAplKey(new AplWalletKey(SECRET.getBytes()));
        wallet.addEthKey(new EthWalletKey(SECRET.getBytes()));
        when(kmsService.getWalletInfo(ACCOUNT_ID_WITH_SECRET, PASSPHRASE)).thenReturn(new WalletKeysInfo(wallet, PASSPHRASE));

        MockHttpResponse response = sendPostRequest("/keyStore/accountInfo", "account=" + Long.toUnsignedString(ACCOUNT_ID_WITH_SECRET)
            + "&passphrase=" + PASSPHRASE);

        assertEquals(200, response.getStatus(), "getAccountInfo response should be always with code 200");
        CurrenciesWalletsDTO expected = new CurrenciesWalletsDTO();
        CurrencyWalletsDTO aplWallets = new CurrencyWalletsDTO("apl", List.of(new WalletDTO(Convert.defaultRsAccount(ACCOUNT_ID_WITH_SECRET), PUBLIC_KEY_SECRET)));
        CurrencyWalletsDTO ethWallets = new CurrencyWalletsDTO("eth", List.of(new WalletDTO(ETH_ADDRESS, ETH_PUBLIC_KEY)));
        expected.getCurrencies().add(aplWallets);
        expected.getCurrencies().add(ethWallets);
        CurrenciesWalletsDTO result = mapper.readValue(response.getContentAsString(), CurrenciesWalletsDTO.class);
        assertEquals(expected, result);
    }
}