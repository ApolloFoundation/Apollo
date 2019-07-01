package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.AccountDTO;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.PublicKey;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.rest.converter.AccountConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.WalletKeysConverter;
import com.apollocurrency.aplwallet.apl.core.rest.service.AccountBalanceService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class AccountControllerTest extends AbstractEndpointTest{

    private static final String PASSPHRASE="123456";
    public static final String PUBLIC_KEY_HEX="e03f00485cabc82491d05297acd9d140f62d61d86f16ba4bcf2a922482a4617d";
    public static final String ACCOUNT_RS="APL-5MRD-NBKX-X5EJ-3UP2M";
    public static final long ACCOUNT_ID = 1838236804542746347L;
    public Account account;
    public AccountDTO accountDTO;

    private AccountController endpoint = new AccountController();

    private AccountConverter accountConverter = mock(AccountConverter.class);
    private AccountService accountService = mock(AccountService.class);
    private AccountAssetService accountAssetService = mock(AccountAssetService.class);
    private AccountCurrencyService accountCurrencyService = mock(AccountCurrencyService.class);
    private AccountBalanceService accountBalanceService = mock(AccountBalanceService.class);


    @BeforeEach
    void setUp() {
        dispatcher = MockDispatcherFactory.createDispatcher();
        dispatcher.getRegistry().addSingletonResource(endpoint);

        endpoint.setConverter(accountConverter);
        endpoint.setWalletKeysConverter(new WalletKeysConverter());
        endpoint.setAccountService(accountService);
        endpoint.setAccountAssetService(accountAssetService);
        endpoint.setAccountCurrencyService(accountCurrencyService);
        endpoint.setAccountBalanceService(accountBalanceService);

        account = new Account(ACCOUNT_ID);
        account.setPublicKey(new PublicKey(ACCOUNT_ID, Convert.parseHexString(PUBLIC_KEY_HEX), 0));
        account.setBalanceATM(1000L);
        account.setForgedBalanceATM(0L);
        account.setUnconfirmedBalanceATM(1500L);

        accountDTO = new AccountDTO(ACCOUNT_ID, ACCOUNT_RS,
                account.getBalanceATM(),
                account.getForgedBalanceATM(),
                account.getUnconfirmedBalanceATM());

    }

    @AfterEach
    void tearDown() {

    }

    @Test
    void getAccount_whenCallWithoutMandatoryParameter_thenGetError_2003() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/account");

        checkMandatoryParameterMissingErrorCode(response, 2003);

    }

    @Test
    void getAccount_whenCallWithWrongAccountId_thenGetError_2005() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/account?account=0");

        checkMandatoryParameterMissingErrorCode(response, 2005);

    }

    @Test
    void getAccount_Account_ID_without_including_additional_information() throws URISyntaxException, IOException {
        doReturn(account).when(accountBalanceService).getAccount(Long.toUnsignedString(ACCOUNT_ID));
        doReturn(accountDTO).when(accountConverter).convert(account);
        MockHttpResponse response = sendGetRequest("/accounts/account?account="+ACCOUNT_ID);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:"+result.get("newErrorCode"));
        assertEquals(Long.toUnsignedString(ACCOUNT_ID), result.get("account"));
        assertEquals(ACCOUNT_RS, result.get("accountRS"));
    }

    @Test
    void createAccount_whenCallWithoutMandatoryParameter_thenGetError_2003() throws URISyntaxException, IOException {
        MockHttpResponse response = sendPostRequest("/accounts/account", "wrong=param");

        checkMandatoryParameterMissingErrorCode(response, 2003);

    }



}