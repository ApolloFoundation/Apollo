package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.AccountDTO;
import com.apollocurrency.aplwallet.api.dto.Status2FA;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.PublicKey;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.app.TwoFactorAuthDetails;
import com.apollocurrency.aplwallet.apl.core.http.TwoFactorAuthParameters;
import com.apollocurrency.aplwallet.apl.core.rest.RestParameters;
import com.apollocurrency.aplwallet.apl.core.rest.converter.Account2FAConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.Account2FADetailsConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.AccountConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.WalletKeysConverter;
import com.apollocurrency.aplwallet.apl.core.rest.service.AccountBalanceService;
import com.apollocurrency.aplwallet.apl.core.rest.utils.Account2FAHelper;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import static org.jboss.resteasy.mock.MockHttpRequest.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

class AccountControllerTest extends AbstractEndpointTest{

    private static final String PASSPHRASE = "123456";
    public static final String PUBLIC_KEY_HEX = "e03f00485cabc82491d05297acd9d140f62d61d86f16ba4bcf2a922482a4617d";
    public static final String ACCOUNT_RS = "APL-5MRD-NBKX-X5EJ-3UP2M";
    public static final long ACCOUNT_ID = 1838236804542746347L;
    public static final String QR_CODE_URL = "https://url.google.com/qrcode";
    public static final String SECRET = "SuperSecret";
    public static final int CODE_2FA = 123456;

    public Account account;
    public AccountDTO accountDTO;

    private AccountController endpoint = new AccountController();

    private AccountConverter accountConverter = mock(AccountConverter.class);
    private AccountService accountService = mock(AccountService.class);
    private AccountAssetService accountAssetService = mock(AccountAssetService.class);
    private AccountCurrencyService accountCurrencyService = mock(AccountCurrencyService.class);
    private AccountBalanceService accountBalanceService = mock(AccountBalanceService.class);

    private Account2FAHelper account2FAHelper = mock(Account2FAHelper.class);


    @BeforeEach
    void setUp() {
        super.setUp();

        dispatcher.getRegistry().addSingletonResource(endpoint);

        endpoint.setConverter(accountConverter);
        endpoint.setWalletKeysConverter(new WalletKeysConverter());
        endpoint.setFaConverter(new Account2FAConverter());
        endpoint.setFaDetailsConverter(new Account2FADetailsConverter());
        endpoint.setAccountService(accountService);
        endpoint.setAccountAssetService(accountAssetService);
        endpoint.setAccountCurrencyService(accountCurrencyService);
        endpoint.setAccountBalanceService(accountBalanceService);
        endpoint.setAccount2FAHelper(account2FAHelper);

        account = new Account(ACCOUNT_ID, 0);
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
        doReturn(account).when(accountBalanceService).retrieveAccountByAccountId(Long.toUnsignedString(ACCOUNT_ID));
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

    @Test
    void enable2FA_withoutMandatoryParameters_thenGetError_2002() throws URISyntaxException, IOException {
        when(account2FAHelper.parse2FARequestParams(null, null,null)).thenCallRealMethod();
        MockHttpResponse response = sendPostRequest("/accounts/enable2FA", "wrong=value");

        checkMandatoryParameterMissingErrorCode(response, 2002);
    }

    @Test
    void enable2FA_withBothSecretPhraseAndPassPhrase_thenGetError_2010() throws URISyntaxException, IOException {
        when(account2FAHelper.parse2FARequestParams(null, PASSPHRASE, SECRET)).thenCallRealMethod();
        MockHttpResponse response = sendPostRequest("/accounts/enable2FA", "passphrase="+PASSPHRASE+"&secretPhrase="+SECRET);

        checkMandatoryParameterMissingErrorCode(response, 2010);
    }

    @Test
    void enable2FA_withoutMandatoryParameter_Account_thenGetError_2003() throws URISyntaxException, IOException {
        when(account2FAHelper.parse2FARequestParams(null, PASSPHRASE, null)).thenCallRealMethod();
        MockHttpResponse response = sendPostRequest("/accounts/enable2FA", "passphrase="+PASSPHRASE);

        checkMandatoryParameterMissingErrorCode(response, 2003);
    }

    @Test
    void enable2FA_withPathPhraseAndAccount() throws URISyntaxException, IOException {
        TwoFactorAuthParameters params2FA = new TwoFactorAuthParameters(ACCOUNT_ID, PASSPHRASE, null);
        TwoFactorAuthDetails authDetails = new TwoFactorAuthDetails(QR_CODE_URL, SECRET, Status2FA.OK);

        doReturn(params2FA).when(account2FAHelper).parse2FARequestParams(ACCOUNT_RS, PASSPHRASE, null);
        doReturn(authDetails).when(account2FAHelper).enable2FA(params2FA);
        MockHttpResponse response = sendPostRequest("/accounts/enable2FA", "passphrase="+PASSPHRASE+"&account="+ACCOUNT_RS);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:"+result.get("newErrorCode"));

        assertEquals(Long.toUnsignedString(ACCOUNT_ID), result.get("account"));
        assertEquals(ACCOUNT_RS, result.get("accountRS"));
        assertEquals(QR_CODE_URL, result.get("qrCodeUrl"));
        assertEquals(SECRET, result.get("secret"));
    }

    @Test
    void enable2FA_withSecretPhrase() throws URISyntaxException, IOException {
        TwoFactorAuthParameters params2FA = new TwoFactorAuthParameters(ACCOUNT_ID, null, SECRET);
        TwoFactorAuthDetails authDetails = new TwoFactorAuthDetails(QR_CODE_URL, SECRET, Status2FA.OK);

        doReturn(params2FA).when(account2FAHelper).parse2FARequestParams(null, null, SECRET);
        doReturn(authDetails).when(account2FAHelper).enable2FA(params2FA);
        MockHttpResponse response = sendPostRequest("/accounts/enable2FA", "secretPhrase="+SECRET);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:"+result.get("newErrorCode"));

        assertEquals(Long.toUnsignedString(ACCOUNT_ID), result.get("account"));
        assertEquals(ACCOUNT_RS, result.get("accountRS"));
        assertEquals(QR_CODE_URL, result.get("qrCodeUrl"));
        assertEquals(SECRET, result.get("secret"));
    }

    @Test
    void disable2FA_withoutRequestAttribute_thenGetError_1000() throws URISyntaxException, IOException {
        check2FA_withoutRequestAttribute_thenGetError_1000("/accounts/disable2FA");
    }

    @Test
    void disable2FA_withoutMandatoryParameters_thenGetError_2002() throws URISyntaxException, IOException {
        check2FA_withoutMandatoryParameters_thenGetError_2002("/accounts/disable2FA");
    }

    @Test
    void disable2FA_withBothSecretPhraseAndPassPhrase_thenGetError_2010() throws URISyntaxException, IOException {
        check2FA_withBothSecretPhraseAndPassPhrase_thenGetError_2010("/accounts/disable2FA");
    }

    @Test
    void disable2FA_withoutMandatoryParameter_Code2FA_thenGetError_2003() throws URISyntaxException, IOException {
        check2FA_withoutMandatoryParameter_Code2FA_thenGetError_2003("/accounts/disable2FA");
    }

    @Test
    void disable2FA_withPathPhraseAndAccountAndCode2FA() throws URISyntaxException, IOException {
        String uri = "/accounts/disable2FA";
        TwoFactorAuthParameters twoFactorAuthParameters = new TwoFactorAuthParameters(ACCOUNT_ID, PASSPHRASE, null);
        twoFactorAuthParameters.setCode2FA(CODE_2FA);


        doReturn(Status2FA.OK).when(account2FAHelper).disable2FA(twoFactorAuthParameters, CODE_2FA);
        check2FA_withPathPhraseAndAccountAndCode2FA(uri, twoFactorAuthParameters);
    }

    @Test
    void disable2FA_withSecretPhraseAndCode2FA() throws URISyntaxException, IOException {
        String uri = "/accounts/disable2FA";
        TwoFactorAuthParameters twoFactorAuthParameters = new TwoFactorAuthParameters(ACCOUNT_ID, null, SECRET);
        twoFactorAuthParameters.setCode2FA(CODE_2FA);

        doReturn(Status2FA.OK).when(account2FAHelper).disable2FA(twoFactorAuthParameters, CODE_2FA);
        check2FA_withSecretPhraseAndCode2FA(uri, twoFactorAuthParameters);
    }

    @Test
    void confirm2FA_withoutRequestAttribute_thenGetError_1000() throws URISyntaxException, IOException {
        check2FA_withoutRequestAttribute_thenGetError_1000("/accounts/confirm2FA");
    }

    @Test
    void confirm2FA_withoutMandatoryParameters_thenGetError_2002() throws URISyntaxException, IOException {
        check2FA_withoutMandatoryParameters_thenGetError_2002("/accounts/confirm2FA");
    }

    @Test
    void confirm2FA_withBothSecretPhraseAndPassPhrase_thenGetError_2010() throws URISyntaxException, IOException {
        check2FA_withBothSecretPhraseAndPassPhrase_thenGetError_2010("/accounts/confirm2FA");
    }

    @Test
    void confirm2FA_withoutMandatoryParameter_Code2FA_thenGetError_2003() throws URISyntaxException, IOException {
        check2FA_withoutMandatoryParameter_Code2FA_thenGetError_2003("/accounts/confirm2FA");
    }

    @Test
    void confirm2FA_withPathPhraseAndAccountAndCode2FA() throws URISyntaxException, IOException {
        String uri = "/accounts/confirm2FA";
        TwoFactorAuthParameters twoFactorAuthParameters = new TwoFactorAuthParameters(ACCOUNT_ID, PASSPHRASE, null);
        twoFactorAuthParameters.setCode2FA(CODE_2FA);


        doReturn(Status2FA.OK).when(account2FAHelper).confirm2FA(twoFactorAuthParameters, CODE_2FA);
        check2FA_withPathPhraseAndAccountAndCode2FA(uri, twoFactorAuthParameters);
    }

    @Test
    void confirm2FA_withSecretPhraseAndCode2FA() throws URISyntaxException, IOException {
        String uri = "/accounts/confirm2FA";
        TwoFactorAuthParameters twoFactorAuthParameters = new TwoFactorAuthParameters(ACCOUNT_ID, null, SECRET);
        twoFactorAuthParameters.setCode2FA(CODE_2FA);

        doReturn(Status2FA.OK).when(account2FAHelper).confirm2FA(twoFactorAuthParameters, CODE_2FA);
        check2FA_withSecretPhraseAndCode2FA(uri, twoFactorAuthParameters);
    }

    private void check2FA_withoutRequestAttribute_thenGetError_1000(String uri) throws URISyntaxException, IOException {
        MockHttpResponse response = sendPostRequest(uri,"wrong=value");

        checkMandatoryParameterMissingErrorCode(response, 1000);
    }

    private void check2FA_withoutMandatoryParameters_thenGetError_2002(String uri) throws URISyntaxException, IOException {
        TwoFactorAuthParameters twoFactorAuthParameters = new TwoFactorAuthParameters(0L, null, null);
        doCallRealMethod().when(account2FAHelper).verify2FA(null, null,null, null);
        doCallRealMethod().when(account2FAHelper).validate2FAParameters(twoFactorAuthParameters);
        doCallRealMethod().when(account2FAHelper).parse2FARequestParams("0", null, null);

        MockHttpRequest request = post(uri);
        request.setAttribute(RestParameters.TWO_FCTOR_AUTH_ATTRIBUTE, twoFactorAuthParameters );

        MockHttpResponse response = sendPostRequest(request,"wrong=value");

        checkMandatoryParameterMissingErrorCode(response, 2003);
    }

    private void check2FA_withBothSecretPhraseAndPassPhrase_thenGetError_2010(String uri) throws URISyntaxException, IOException {
        TwoFactorAuthParameters twoFactorAuthParameters = new TwoFactorAuthParameters(0L, PASSPHRASE, SECRET);
        twoFactorAuthParameters.setCode2FA(CODE_2FA);

        doCallRealMethod().when(account2FAHelper).validate2FAParameters(twoFactorAuthParameters);
        doCallRealMethod().when(account2FAHelper).parse2FARequestParams("0", PASSPHRASE, SECRET);

        MockHttpRequest request = post(uri);
        request.setAttribute(RestParameters.TWO_FCTOR_AUTH_ATTRIBUTE, twoFactorAuthParameters );

        MockHttpResponse response = sendPostRequest(request,"passphrase="+PASSPHRASE+"&secretPhrase="+SECRET+"&code2FA="+CODE_2FA);

        checkMandatoryParameterMissingErrorCode(response, 2010);
    }

    private void check2FA_withoutMandatoryParameter_Code2FA_thenGetError_2003(String uri) throws URISyntaxException, IOException {
        TwoFactorAuthParameters twoFactorAuthParameters = new TwoFactorAuthParameters(0L, null, SECRET);
        doCallRealMethod().when(account2FAHelper).validate2FAParameters(twoFactorAuthParameters);

        MockHttpRequest request = post(uri);
        request.setAttribute(RestParameters.TWO_FCTOR_AUTH_ATTRIBUTE, twoFactorAuthParameters );

        MockHttpResponse response = sendPostRequest(request,"secretPhrase="+SECRET);

        checkMandatoryParameterMissingErrorCode(response, 2003);
    }

    private void check2FA_withPathPhraseAndAccountAndCode2FA(String uri, TwoFactorAuthParameters twoFactorAuthParameters) throws URISyntaxException, IOException {
        doCallRealMethod().when(account2FAHelper).validate2FAParameters(twoFactorAuthParameters);

        MockHttpRequest request = post(uri);
        request.setAttribute(RestParameters.TWO_FCTOR_AUTH_ATTRIBUTE, twoFactorAuthParameters );
        MockHttpResponse response = sendPostRequest(request, "passphrase="+PASSPHRASE+"&account="+ACCOUNT_RS+"&code2FA="+CODE_2FA);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:"+result.get("newErrorCode"));

        assertEquals(Long.toUnsignedString(ACCOUNT_ID), result.get("account"));
        assertEquals(ACCOUNT_RS, result.get("accountRS"));
    }

    private void check2FA_withSecretPhraseAndCode2FA(String uri, TwoFactorAuthParameters twoFactorAuthParameters) throws URISyntaxException, IOException {
        doCallRealMethod().when(account2FAHelper).validate2FAParameters(twoFactorAuthParameters);

        MockHttpRequest request = post(uri);
        request.setAttribute(RestParameters.TWO_FCTOR_AUTH_ATTRIBUTE, twoFactorAuthParameters );

        MockHttpResponse response = sendPostRequest(request,"secretPhrase="+SECRET+"&code2FA="+CODE_2FA);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:"+result.get("newErrorCode"));

        assertEquals(Long.toUnsignedString(ACCOUNT_ID), result.get("account"));
        assertEquals(ACCOUNT_RS, result.get("accountRS"));
    }

}